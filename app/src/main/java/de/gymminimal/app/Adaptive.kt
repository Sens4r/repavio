package de.gymminimal.app

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.pow
import kotlin.math.roundToInt

/*
 * Ernährung Stufe 3: adaptiver Bedarf.
 * Der tatsächliche Verbrauch ergibt sich aus Energiebilanz: Ø Aufnahme − Gewichtsänderung × 7700 kcal/kg.
 * Gewicht wird geglättet (exponentiell, 10 % pro Tag), damit Wassertage nicht durchschlagen.
 * Nur gespeicherte Tage zählen: Tage ohne Tagebucheintrag fließen nicht ein (keine Nullwerte).
 * Das Ziel ändert sich nur einmal pro Woche (montags), damit es im Alltag ruhig bleibt.
 */

const val ADAPT_WINDOW_DAYS = 28
const val ADAPT_MIN_SPAN_DAYS = 14
const val ADAPT_MIN_LOGGED_DAYS = 10
const val ADAPT_FULL_CONFIDENCE_DAYS = 21
const val KCAL_PER_KG = 7700.0
const val TREND_ALPHA = 0.1
/** Die Datenkorrektur bleibt innerhalb ±25 % der Formel. */
const val ADAPT_MAX_SHIFT = 0.25

data class TrendPoint(val day: LocalDate, val scale: Double, val trend: Double)

data class Adaptive(
    val formulaKcal: Int,
    /** Verbrauch rein aus den Daten, falls berechenbar. */
    val dataKcal: Int?,
    /** Verwendeter Erhaltungsbedarf: Formel + gewichtete Korrektur. */
    val estimatedKcal: Int,
    val kgPerWeek: Double?,
    val trendKg: Double?,
    val loggedDays: Int,
    val spanDays: Int,
    val ready: Boolean,
    /** Wie viele Tage noch fehlen, bis die Anpassung greift (0 = bereit). */
    val daysMissing: Int
) {
    val shift: Int get() = estimatedKcal - formulaKcal
}

fun dayOfTimestamp(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

/** Geglätteter Gewichtsverlauf; Lücken zwischen Wiegungen werden über die Tageszahl berücksichtigt. */
fun weightTrend(weights: List<Pair<LocalDate, Double>>): List<TrendPoint> {
    val sorted = weights.sortedBy { it.first }
    var trend: Double? = null
    var last: LocalDate? = null
    return sorted.map { (day, kg) ->
        val t = trend
        val next = if (t == null) kg else {
            val gap = ChronoUnit.DAYS.between(last, day).coerceAtLeast(1)
            val alpha = 1 - (1 - TREND_ALPHA).pow(gap.toDouble())
            t + alpha * (kg - t)
        }
        trend = next; last = day
        TrendPoint(day, kg, next)
    }
}

/** Aufnahme je Tag (nur Tage mit Einträgen). */
fun intakeByDay(log: FoodLog): Map<LocalDate, Double> =
    log.entries.groupBy { it.day }.mapNotNull { (day, list) ->
        runCatching { LocalDate.parse(day) }.getOrNull()?.let { it to list.sumOf { e -> e.values.kcal } }
    }.toMap().filterValues { it > 0 }

/** Schätzung zum Stichtag asOf (einschließlich) aus den letzten 28 Tagen. */
fun estimateMaintenance(formulaKcal: Int, weights: List<Pair<LocalDate, Double>>,
                        intake: Map<LocalDate, Double>, asOf: LocalDate): Adaptive {
    val from = asOf.minusDays(ADAPT_WINDOW_DAYS - 1L)
    val trend = weightTrend(weights.filter { !it.first.isAfter(asOf) })
    val inWindow = trend.filter { !it.day.isBefore(from) }
    val logged = intake.filterKeys { !it.isBefore(from) && !it.isAfter(asOf) }
    val span = if (inWindow.size >= 2) ChronoUnit.DAYS.between(inWindow.first().day, inWindow.last().day).toInt() else 0
    val ready = inWindow.size >= 3 && span >= ADAPT_MIN_SPAN_DAYS && logged.size >= ADAPT_MIN_LOGGED_DAYS
    val missing = maxOf(ADAPT_MIN_SPAN_DAYS - span, ADAPT_MIN_LOGGED_DAYS - logged.size, if (inWindow.size >= 3) 0 else 1, 0)
    val perDay = if (span > 0) (inWindow.last().trend - inWindow.first().trend) / span else null
    val data = if (ready && perDay != null) logged.values.average() - perDay * KCAL_PER_KG else null
    val estimated = if (data == null) formulaKcal.toDouble() else {
        val confidence = (logged.size.toDouble() / ADAPT_FULL_CONFIDENCE_DAYS).coerceAtMost(1.0)
        val limit = formulaKcal * ADAPT_MAX_SHIFT
        formulaKcal + (confidence * (data - formulaKcal)).coerceIn(-limit, limit)
    }
    return Adaptive(formulaKcal, data?.roundToInt(), estimated.roundToInt(), perDay?.times(7),
        trend.lastOrNull()?.trend, logged.size, span, ready, if (ready) 0 else missing)
}

fun weekStart(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

data class NutritionTarget(
    /** Gilt diese Woche (Daten bis Sonntag der Vorwoche). */
    val result: NutritionResult,
    val current: Adaptive,
    /** Vorschau für den nächsten Montag (Daten bis heute). */
    val next: Adaptive,
    val nextKcal: Int
) {
    val adaptive: Boolean get() = current.ready
}

private fun withMaintenance(base: NutritionResult, maintenance: Int): NutritionResult {
    val kcal = (maintenance * base.goal.factor).roundToInt()
    val carbs = ((kcal - base.proteinG * 4 - base.fatG * 9) / 4.0).roundToInt().coerceAtLeast(0)
    return base.copy(maintenance = maintenance, kcal = kcal, carbsG = carbs)
}

/** Ziel fürs Tagebuch: Formel, sobald genug Daten da sind wöchentlich an den tatsächlichen Verbrauch angepasst. */
fun nutritionTarget(profile: Profile, log: FoodLog, today: LocalDate = LocalDate.now()): NutritionTarget? {
    val base = nutritionFor(profile, today) ?: return null
    val weights = profile.weights.map { dayOfTimestamp(it.timestamp) to it.kg }
    val intake = intakeByDay(log)
    val current = estimateMaintenance(base.maintenance, weights, intake, weekStart(today).minusDays(1))
    val next = estimateMaintenance(base.maintenance, weights, intake, today)
    val result = if (current.ready) withMaintenance(base, current.estimatedKcal) else base
    val nextKcal = (next.estimatedKcal * base.goal.factor).roundToInt()
    return NutritionTarget(result, current, next, nextKcal)
}
