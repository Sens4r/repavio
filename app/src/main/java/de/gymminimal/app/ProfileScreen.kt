package de.gymminimal.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

fun avatarIcon(key: String): ImageVector = when (key) {
    "hantel" -> Icons.Outlined.FitnessCenter
    "blitz" -> Icons.Outlined.Bolt
    "flamme" -> Icons.Outlined.LocalFireDepartment
    "berg" -> Icons.Outlined.Landscape
    "lauf" -> Icons.Outlined.DirectionsRun
    "herz" -> Icons.Outlined.FavoriteBorder
    "stern" -> Icons.Outlined.StarOutline
    else -> Icons.Outlined.Eco
}

private fun kgText(kg: Double): String =
    String.format(Locale.GERMANY, if (kg % 1.0 == 0.0) "%.0f" else "%.1f", kg)

private fun dateText(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY))

/** Photo > chosen avatar > first letter of the name > person icon. */
@Composable
fun ProfileAvatar(profile: Profile, photo: ImageBitmap?, size: Dp) {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.size(size).clip(RoundedCornerShape(size / 2)).background(scheme.primaryContainer),
        contentAlignment = Alignment.Center) {
        when {
            profile.hasPhoto && photo != null -> Image(photo, null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize())
            profile.avatar.isNotBlank() -> Icon(avatarIcon(profile.avatar), null,
                tint = scheme.onPrimaryContainer, modifier = Modifier.size(size * .5f))
            profile.name.isNotBlank() -> Text(profile.name.trim().take(1).uppercase(Locale.GERMANY),
                color = scheme.onPrimaryContainer, fontSize = (size.value * .42f).sp, fontWeight = FontWeight.ExtraBold)
            else -> Icon(Icons.Outlined.Person, null, tint = scheme.onPrimaryContainer,
                modifier = Modifier.size(size * .5f))
        }
    }
}

/** Header and body data of the profile tab. Settings follow below it (SettingsScreen). */
fun LazyListScope.profileOverview(
    profile: Profile, photo: ImageBitmap?,
    onAvatar: () -> Unit, onEdit: () -> Unit, onWeight: () -> Unit
) {
    item {
        val age = ageOn(profile.birthDate)
        val subtitle = listOfNotNull(age?.let { "$it Jahre" },
            when (profile.sex) { "m" -> "männlich"; "w" -> "weiblich"; else -> null })
            .joinToString(" · ").ifBlank { "Angaben ergänzen" }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CardBackground)
            .padding(start = 18.dp, end = 8.dp, top = 18.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clip(RoundedCornerShape(36.dp)).clickable(onClickLabel = "Profilbild ändern", onClick = onAvatar)) {
                ProfileAvatar(profile, photo, 72.dp)
                Box(Modifier.align(Alignment.BottomEnd).size(26.dp).clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.PhotoCamera, null, tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(15.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(profile.name.ifBlank { "Dein Profil" }, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, fontSize = 13.sp, color = SubtleText)
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.Edit, "Name und Angaben bearbeiten", tint = SubtleText, modifier = Modifier.size(20.dp))
            }
        }
    }
    item {
        val tiles = listOf(
            Triple("Größe", if (validHeight(profile.heightCm)) "${profile.heightCm}" else "–", " cm"),
            Triple("Gewicht", profile.currentWeight?.let(::kgText) ?: "–", " kg"),
            Triple("Alter", ageOn(profile.birthDate)?.toString() ?: "–", " Jahre"),
            Triple("Aktivität", activityLevel(profile.activity).label, "")
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tiles.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { (label, value, unit) ->
                        Column(Modifier.weight(1f).height(76.dp).clip(RoundedCornerShape(20.dp))
                            .background(CardBackground)
                            .clickable(onClickLabel = "$label ändern") { if (label == "Gewicht") onWeight() else onEdit() }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.SpaceBetween) {
                            Text(label, fontSize = 12.sp, color = SubtleText)
                            Text(buildAnnotatedString {
                                append(value)
                                if (value != "–") withStyle(SpanStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                    color = SubtleText)) { append(unit) }
                            }, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
    item {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, null, tint = SubtleText, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Alle Angaben bleiben verschlüsselt auf diesem Gerät.", fontSize = 11.sp, color = SubtleText)
        }
    }
}

@Composable
fun AvatarDialog(profile: Profile, onPickPhoto: () -> Unit, onAvatar: (String) -> Unit,
                 onRemove: () -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(onDismissRequest = onDismiss, containerColor = CardBackground, shape = RoundedCornerShape(28.dp),
        title = { Text("Profilbild", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Button(onClick = onPickPhoto, modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp)) {
                    Icon(Icons.Outlined.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Foto wählen", fontWeight = FontWeight.Bold)
                }
                Text("Oder ein Avatar", fontSize = 13.sp, color = SubtleText)
                AVATARS.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { option ->
                            val active = !profile.hasPhoto && profile.avatar == option.key
                            Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(18.dp))
                                .background(if (active) scheme.primaryContainer else FieldBackground)
                                .then(if (active) Modifier.border(2.dp, scheme.primary, RoundedCornerShape(18.dp)) else Modifier)
                                .selectable(selected = active, onClick = { onAvatar(option.key) }, role = Role.RadioButton),
                                contentAlignment = Alignment.Center) {
                                Icon(avatarIcon(option.key), option.label,
                                    tint = if (active) scheme.onPrimaryContainer else scheme.onSurface,
                                    modifier = Modifier.size(26.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fertig") } },
        dismissButton = if (profile.hasPhoto || profile.avatar.isNotBlank()) ({
            TextButton(onClick = onRemove) { Text("Entfernen") }
        }) else null)
}

@Composable
fun ProfileEditDialog(profile: Profile, onSave: (Profile) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(profile.name) }
    var birth by remember { mutableStateOf(formatBirthDate(profile.birthDate)) }
    var sex by remember { mutableStateOf(profile.sex) }
    var height by remember { mutableStateOf(if (profile.heightCm > 0) profile.heightCm.toString() else "") }
    var activity by remember { mutableStateOf(profile.activity) }
    val birthIso = if (birth.isBlank()) "" else parseBirthDate(birth)
    val heightCm = if (height.isBlank()) 0 else height.toIntOrNull()?.takeIf(::validHeight)
    val valid = birthIso != null && heightCm != null && name.length <= 40
    val scheme = MaterialTheme.colorScheme
    AlertDialog(onDismissRequest = onDismiss, containerColor = CardBackground, shape = RoundedCornerShape(28.dp),
        title = { Text("Profil bearbeiten", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it.take(40) }, label = { Text("Name") }, singleLine = true,
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(birth, { birth = it.filter { c -> c.isDigit() || c == '.' }.take(10) },
                    label = { Text("Geburtsdatum") }, placeholder = { Text("TT.MM.JJJJ") }, singleLine = true,
                    isError = birthIso == null,
                    supportingText = { Text(if (birthIso == null) "Bitte als TT.MM.JJJJ eingeben"
                        else "Das Alter wird daraus automatisch berechnet.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                Text("Geschlecht", fontSize = 12.sp, color = SubtleText)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("m" to "männlich", "w" to "weiblich").forEach { (key, label) ->
                        FilterChip(selected = sex == key, onClick = { sex = key },
                            label = { Text(label, fontWeight = FontWeight.Bold) }, shape = RoundedCornerShape(18.dp),
                            border = null, colors = FilterChipDefaults.filterChipColors(
                                containerColor = FieldBackground, selectedContainerColor = scheme.primary,
                                selectedLabelColor = scheme.onPrimary))
                    }
                }
                OutlinedTextField(height, { height = it.filter(Char::isDigit).take(3) },
                    label = { Text("Größe (cm)") }, singleLine = true, isError = heightCm == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                Text("Aktivität im Alltag", fontSize = 12.sp, color = SubtleText)
                ACTIVITY_LEVELS.forEach { level ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .selectable(selected = activity == level.key, onClick = { activity = level.key },
                            role = Role.RadioButton).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = activity == level.key, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(level.label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(level.detail, fontSize = 12.sp, color = SubtleText)
                        }
                    }
                }
                Text("Das Gewicht trägst du über die Kachel „Gewicht“ ein, damit der Verlauf erhalten bleibt.",
                    fontSize = 11.sp, color = SubtleText)
            }
        },
        confirmButton = {
            Button(enabled = valid, shape = RoundedCornerShape(24.dp), onClick = {
                onSave(profile.copy(name = name.trim(), birthDate = birthIso!!, sex = sex,
                    heightCm = heightCm!!, activity = activity))
            }) { Text("Speichern", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } })
}

/** Weight history: chart and list of real weighings, newest first. */
fun LazyListScope.weightPage(profile: Profile, onAdd: () -> Unit, onDelete: (WeightEntry) -> Unit) {
    val entries = profile.weights.sortedBy { it.timestamp }
    item {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CardBackground).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("AKTUELL", color = SubtleText, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(buildAnnotatedString {
                        append(profile.currentWeight?.let(::kgText) ?: "–")
                        withStyle(SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SubtleText)) { append(" kg") }
                    }, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp, lineHeight = 40.sp)
                }
                if (entries.size > 1) {
                    val delta = entries.last().kg - entries.first().kg
                    val sign = if (delta > 0) "+" else if (delta < 0) "−" else "±"
                    Text("$sign${kgText(abs(delta))} kg seit Beginn", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = 4.dp).clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
            if (entries.size >= 2) WeightChart(entries.map { it.kg })
            else Text("Der Verlauf erscheint ab zwei Einträgen.", fontSize = 12.sp, color = SubtleText)
        }
    }
    item {
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(52.dp),
            shape = RoundedCornerShape(26.dp)) {
            Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Gewicht eintragen", fontWeight = FontWeight.Bold)
        }
    }
    if (entries.isNotEmpty()) item {
        Text("Verlauf", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 2.dp, top = 10.dp))
    }
    items(entries.asReversed(), key = { it.timestamp }) { entry ->
        Row(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(CardBackground)
            .padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(dateText(entry.timestamp), fontSize = 13.sp, color = SubtleText, modifier = Modifier.weight(1f))
            Text("${kgText(entry.kg)} kg", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { onDelete(entry) }, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Outlined.DeleteOutline, "Eintrag vom ${dateText(entry.timestamp)} löschen",
                    tint = SubtleText, modifier = Modifier.size(19.dp))
            }
        }
    }
}

@Composable
private fun WeightChart(values: List<Double>) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(130.dp)) {
        val lo = (values.minOrNull() ?: 0.0) - 1.0
        val hi = (values.maxOrNull() ?: 0.0) + 1.0
        val pad = 8.dp.toPx()
        fun point(i: Int) = Offset(pad + i * (size.width - pad * 2) / (values.size - 1).coerceAtLeast(1),
            pad + ((hi - values[i]) / (hi - lo) * (size.height - pad * 2)).toFloat())
        val line = Path().apply { values.indices.forEach { i -> point(i).let { if (i == 0) moveTo(it.x, it.y) else lineTo(it.x, it.y) } } }
        drawPath(line, color, style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        values.indices.forEach { drawCircle(color, radius = 3.dp.toPx(), center = point(it)) }
        drawCircle(color, radius = 6.dp.toPx(), center = point(values.lastIndex))
        drawCircle(CardBackground, radius = 2.5.dp.toPx(), center = point(values.lastIndex))
    }
}

@Composable
fun WeightDialog(current: Double?, onSave: (Double) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current?.let(::kgText) ?: "") }
    val kg = text.replace(',', '.').toDoubleOrNull()?.takeIf(::validWeight)
    AlertDialog(onDismissRequest = onDismiss, containerColor = CardBackground, shape = RoundedCornerShape(28.dp),
        title = { Text("Gewicht heute", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(text, { text = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(6) },
                    label = { Text("Gewicht (kg)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                Text("Ein zweiter Eintrag am selben Tag ersetzt den ersten.", fontSize = 11.sp, color = SubtleText)
            }
        },
        confirmButton = {
            Button(enabled = kg != null, shape = RoundedCornerShape(24.dp), onClick = { onSave(kg!!) }) {
                Text("Speichern", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } })
}

@Composable
fun BackupPasswordDialog(hasPassword: Boolean, busy: Boolean, onSet: (String) -> Unit,
                         onRemove: () -> Unit, onDismiss: () -> Unit) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    val valid = first.length >= 8 && first == second
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, containerColor = CardBackground,
        shape = RoundedCornerShape(28.dp),
        title = { Text(if (hasPassword) "Backup-Passwort ändern" else "Backup-Passwort", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Mit Passwort werden Profil und Ernährungstagebuch verschlüsselt in Backups aufgenommen und lassen sich auch " +
                    "auf einem neuen Handy wiederherstellen. Ohne das Passwort sind sie nicht lesbar – auch nicht für dich.",
                    fontSize = 13.sp, color = SubtleText)
                OutlinedTextField(first, { first = it }, label = { Text("Passwort (mind. 8 Zeichen)") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(second, { second = it }, label = { Text("Wiederholen") }, singleLine = true,
                    isError = second.isNotEmpty() && second != first,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (hasPassword) TextButton(onClick = onRemove, enabled = !busy, contentPadding = PaddingValues(0.dp)) {
                    Text("Passwort entfernen – Profil nicht mehr mitsichern", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(enabled = valid && !busy, shape = RoundedCornerShape(24.dp), onClick = { onSet(first) }) {
                Text("Festlegen", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Abbrechen") } })
}

@Composable
fun RestoreProfileDialog(busy: Boolean, failed: Boolean, onRestore: (String) -> Unit, onSkip: () -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = { if (!busy) onSkip() }, containerColor = CardBackground,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Profil wiederherstellen?", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Die Trainingsdaten sind wiederhergestellt. Das Backup enthält zusätzlich verschlüsselte persönliche Daten (Profil, Ernährung).",
                    fontSize = 13.sp, color = SubtleText)
                OutlinedTextField(password, { password = it }, label = { Text("Backup-Passwort") }, singleLine = true,
                    isError = failed, supportingText = if (failed) ({ Text("Passwort falsch oder Daten beschädigt.") }) else null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(enabled = password.isNotEmpty() && !busy, shape = RoundedCornerShape(24.dp),
                onClick = { onRestore(password) }) { Text("Wiederherstellen", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onSkip) { Text("Ohne Profil") } })
}
