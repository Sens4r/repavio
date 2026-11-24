package de.gymminimal.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Offline-only JSON in private Android app storage. No internet permission. */
class LocalStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("gymminimal_local_v1", Context.MODE_PRIVATE)

    fun load(): AppData {
        val raw = prefs.getString("state", null) ?: return initialData()
        return try {
            val json = JSONObject(raw)
            val migrated = decode(json)
            // Persist migration only once; stable IDs must survive restarts.
            if (json.optInt("schema", 0) < 6) {
                // Keep an untouched V1.1 safety snapshot before upgrading legacy exercise IDs.
                try { BackupManager(context).createManualSafetyCopy(raw) } catch (_: Exception) { }
                save(migrated)
            }
            migrated
        } catch (_: Exception) { initialData() }
    }

    fun clear() { prefs.edit().clear().commit() }

    fun introductionSeen(): Boolean = prefs.getBoolean("introduction_seen", false)
    fun setIntroductionSeen(value: Boolean) { prefs.edit().putBoolean("introduction_seen", value).apply() }


    fun save(data: AppData) {
        prefs.edit().putString("state", encode(data).toString()).apply()
    }

    /** Training data plus, only if a backup password is set, the encrypted profile. */
    fun exportJson(): String = encode(load()).apply {
        ProfileStore(context).backupBlob()?.let { put("profileEncrypted", it) }
    }.toString(2)

    /** Refuse invalid backups without touching existing data. */
    fun importJson(json: String): AppData {
        val root = JSONObject(json)
        val data = decode(root)
        require((data.phase.weeks in 1..52 || data.phase.weeks == 0 && (data.phase.openEnded || !data.phase.isConfigured)) &&
            data.plans.size == 7)
        require(data.history.size < 100000 && data.plans.all { it.day in 0..6 })
        BackupManager(context).createManualSafetyCopy(exportJson())
        save(data)
        return data
    }

    private fun JSONObject.optBoolNull(key: String): Boolean? = if (has(key) && !isNull(key)) optBoolean(key) else null
    private fun JSONObject.optFinite(key: String): Double = optDouble(key, 0.0).takeIf { it.isFinite() && it >= 0 } ?: 0.0
    fun loadTabs(): TabConfig = decodeTabs(prefs.getString("tabs", null))
    fun saveTabs(config: TabConfig) { prefs.edit().putString("tabs", encodeTabs(config)).apply() }

    private fun JSONObject.text(key: String, fallback: String = "") = optString(key, fallback)
    private fun <T> JSONArray.collectItems(fn: (JSONObject) -> T): List<T> =
        (0 until length()).map { fn(getJSONObject(it)) }

    private fun encode(data: AppData) = JSONObject().apply {
        put("schema", 6)
        put("phase", JSONObject().apply {
            put("id", data.phase.id)
            put("name", data.phase.name)
            put("weeks", data.phase.weeks)
            put("unitsPerWeek", data.phase.unitsPerWeek)
            data.phase.finishedAt?.let { put("finishedAt", it) }
            data.phase.archivedAt?.let { put("archivedAt", it) }
            if (data.phase.openEnded) put("openEnded", true)
        })
        put("pastPhases", JSONArray().apply {
            data.pastPhases.forEach { phase -> put(JSONObject().apply {
                put("id", phase.id); put("name", phase.name)
                put("weeks", phase.weeks); put("unitsPerWeek", phase.unitsPerWeek)
                phase.finishedAt?.let { put("finishedAt", it) }
                phase.archivedAt?.let { put("archivedAt", it) }
                if (phase.openEnded) put("openEnded", true)
            }) }
        })
        put("customTags", JSONArray().apply { data.customTags.forEach { put(it) } })
        put("exerciseCatalog", JSONArray().apply {
            data.exerciseCatalog.forEach { definition -> put(JSONObject().apply {
                put("id", definition.id); put("name", definition.name)
            }) }
        })
        put("plans", JSONArray().apply {
            data.plans.forEach { plan -> put(JSONObject().apply {
                put("day", plan.day)
                put("muscles", plan.muscles)
                put("enabled", plan.enabled)
                put("exercises", JSONArray().apply {
                    plan.exercises.forEach { ex -> put(JSONObject().apply {
                        put("id", ex.id); put("name", ex.name)
                        put("exerciseKey", ex.exerciseKey)
                        put("weight", ex.weight); put("reps", ex.reps)
                        put("sets", ex.sets); put("rest", ex.rest)
                        put("muscleGroup", ex.muscleGroup)
                        ex.bodyweight?.let { put("bodyweight", it) }
                        put("alternatives", JSONArray().apply {
                            ex.alternatives.forEach { a -> put(JSONObject().apply {
                                put("id", a.id); put("name", a.name)
                                put("exerciseKey", a.exerciseKey)
                                put("weight", a.weight); put("reps", a.reps)
                                put("sets", a.sets); put("rest", a.rest)
                                a.bodyweight?.let { put("bodyweight", it) }
                            }) }
                        })
                    }) }
                })
            }) }
        })
        put("history", JSONArray().apply {
            data.history.forEach { work -> put(JSONObject().apply {
                put("id", work.id); put("phaseId", work.phaseId)
                put("planDay", work.planDay); put("timestamp", work.timestamp)
                work.durationMs?.let { put("durationMs", it) }
                put("entries", JSONArray().apply {
                    work.entries.forEach { e -> put(JSONObject().apply {
                        put("originalId", e.originalId); put("performedId", e.performedId)
                        put("name", e.name); put("status", e.status)
                        put("weight", e.weight); put("reps", e.reps); put("sets", e.sets)
                        put("muscleGroup", e.muscleGroup)
                        if (e.bodyweight) {
                            put("bodyweight", true); put("bodyKg", e.bodyKg); put("extraKg", e.extraKg)
                            put("bandKg", e.bandKg); put("bandName", e.bandName); put("bandId", e.bandId)
                        }
                    }) }
                })
            }) }
        })
        put("bands", JSONArray().apply {
            data.bands.forEach { b -> put(JSONObject().apply { put("id", b.id); put("color", b.color); put("kg", b.kg) }) }
        })
        put("choices", JSONObject().apply {
            data.choices.forEach { (key, c) -> put(key, JSONObject().apply {
                put("status", c.status); put("alternativeId", c.alternativeId)
            }) }
        })
        put("drafts", JSONObject().apply {
            data.drafts.forEach { (key, v) -> put(key, JSONObject().apply {
                put("weight", v.weight); put("reps", v.reps)
            }) }
        })
    }

    private fun decodePhase(json: JSONObject) = Phase(
        id = json.getString("id"), name = json.getString("name"),
        weeks = json.getInt("weeks"), unitsPerWeek = json.getInt("unitsPerWeek"),
        finishedAt = if (json.has("finishedAt") && !json.isNull("finishedAt"))
            json.getLong("finishedAt") else null,
        archivedAt = if (json.has("archivedAt") && !json.isNull("archivedAt"))
            json.getLong("archivedAt") else null,
        openEnded = json.optBoolean("openEnded", false)
    )

    private fun decode(root: JSONObject): AppData {
        val phase = decodePhase(root.getJSONObject("phase"))
        val pastPhases = root.optJSONArray("pastPhases")?.collectItems(::decodePhase) ?: emptyList()
        val plansByDay = root.getJSONArray("plans").collectItems { plan ->
            DayPlan(day = plan.getInt("day"), muscles = plan.text("muscles"),
                enabled = if (plan.has("enabled")) plan.optBoolean("enabled") else
                    plan.text("muscles").isNotBlank() || plan.getJSONArray("exercises").length() > 0,
                exercises = plan.getJSONArray("exercises").collectItems { ex ->
                    Exercise(
                        id = ex.getString("id"), name = ex.getString("name"),
                        exerciseKey = ex.text("exerciseKey"),
                        weight = ex.text("weight", "0"), reps = ex.text("reps", "10"),
                        sets = ex.getInt("sets"), rest = ex.getInt("rest"),
                        bodyweight = ex.optBoolNull("bodyweight"),
                        muscleGroup = normalizeMuscleGroup(ex.text("muscleGroup", inferredMuscleGroup(ex.getString("name"),
                            plan.text("muscles").split("·", ",").firstOrNull()?.trim().orEmpty()))),
                        alternatives = ex.getJSONArray("alternatives").collectItems { a ->
                            Alternative(a.getString("id"), a.getString("name"),
                                a.text("weight", "0"), a.text("reps", "10"),
                                a.getInt("sets"), a.getInt("rest"), a.text("exerciseKey"), a.optBoolNull("bodyweight"))
                        }
                    )
                })
        }.associateBy { it.day }
        val plans = (0..6).map { plansByDay[it] ?: DayPlan(it) }
        val history = root.getJSONArray("history").collectItems { work ->
            Workout(id = work.getString("id"), phaseId = work.getString("phaseId"),
                planDay = work.getInt("planDay"), timestamp = work.getLong("timestamp"),
                durationMs = work.optLong("durationMs", -1L).takeIf { it > 0 },
                entries = work.getJSONArray("entries").collectItems { e ->
                    LoggedExercise(e.getString("originalId"), e.getString("performedId"),
                        e.getString("name"), e.getString("status"),
                        e.getDouble("weight"), e.getInt("reps"), e.getInt("sets"),
                        normalizeMuscleGroup(e.text("muscleGroup", inferredMuscleGroup(e.getString("name")))),
                        bodyweight = e.optBoolean("bodyweight", false),
                        bodyKg = e.optFinite("bodyKg"), extraKg = e.optFinite("extraKg"), bandKg = e.optFinite("bandKg"),
                        bandName = e.text("bandName").take(20), bandId = e.text("bandId").take(60))
                })
        }
        val choicesJson = root.optJSONObject("choices") ?: JSONObject()
        val choices = choicesJson.keys().asSequence().associateWith { key ->
            val c = choicesJson.getJSONObject(key)
            ExerciseChoice(c.text("status", "completed"), c.text("alternativeId"))
        }
        val draftsJson = root.optJSONObject("drafts") ?: JSONObject()
        val drafts = draftsJson.keys().asSequence().associateWith { key ->
            val v = draftsJson.getJSONObject(key)
            Values(v.text("weight"), v.text("reps"))
        }
        val customTags = root.optJSONArray("customTags")?.let { arr ->
            (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }.distinct()
        } ?: emptyList()
        // Preserve previously used nonstandard categories as editable custom tags.
        // In V0.5 Crossfit, Trizeps and Sonstige were fixed options; they are no
        // longer defaults. Historic logs are never rewritten.
        val referenced = plans.flatMap { plan ->
            chosenMuscles(plan.muscles).toList() + plan.exercises.map { it.muscleGroup }
        }.filter { name -> name.isNotBlank() && name !in MUSCLE_GROUPS && name != "Sonstige" }
        val bands = root.optJSONArray("bands")?.let { arr ->
            (0 until minOf(arr.length(), 50)).mapNotNull { i ->
                arr.optJSONObject(i)?.let { Band(it.optString("id").ifBlank { makeId() }, it.optString("color"), it.optDouble("kg")) }
                    ?.takeIf(::validBand)
            }
        } ?: emptyList()
        val catalog = root.optJSONArray("exerciseCatalog")?.collectItems { definition ->
            ExerciseDefinition(definition.getString("id"), definition.getString("name"))
        } ?: emptyList()
        return migrateExerciseIdentities(AppData(phase, plans, history, choices, drafts, pastPhases,
            alphabeticTags(customTags + referenced.filterNot { it in customTags }), catalog, bands))
    }
}
