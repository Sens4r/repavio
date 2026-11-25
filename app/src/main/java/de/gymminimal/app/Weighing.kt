package de.gymminimal.app

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Gewichtsabfrage: kurzes Pop-up beim Öffnen der App, wenn eine Wiegung fällig ist.
 * Erst aktiv, wenn im Profil schon ein Gewicht steht. Wer am Tag schon gewogen hat, wird nicht gefragt;
 * „Heute nicht“ gilt nur für den Tag. Gespeichert lokal (LocalStore), nicht im Backup.
 */
data class WeighConfig(val frequency: String = "weekly", val day: Int = 0) // day: 0 = Montag … 6 = Sonntag

val WEIGH_FREQUENCIES = listOf(
    "off" to "Aus", "daily" to "Täglich", "3d" to "Alle 3 Tage", "weekly" to "Wöchentlich", "2w" to "Alle 2 Wochen"
)
val WEEKDAY_SHORT = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
val WEEKDAY_LONG = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")

fun weighUsesDay(frequency: String) = frequency == "weekly" || frequency == "2w"

fun encodeWeighConfig(c: WeighConfig): String = "${c.frequency}:${c.day}"
fun decodeWeighConfig(raw: String?): WeighConfig {
    val parts = raw?.split(':') ?: return WeighConfig()
    val f = parts.getOrNull(0)?.takeIf { k -> WEIGH_FREQUENCIES.any { it.first == k } } ?: "weekly"
    val d = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..6 } ?: 0
    return WeighConfig(f, d)
}

fun weighDue(config: WeighConfig, weights: List<WeightEntry>, today: LocalDate, snoozedOn: LocalDate?): Boolean {
    if (config.frequency == "off" || weights.isEmpty() || snoozedOn == today) return false
    val last = weights.maxOf { dayOfTimestamp(it.timestamp) }
    if (!last.isBefore(today)) return false
    val sinceLast = ChronoUnit.DAYS.between(last, today)
    val anchor = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(config.day + 1)))
    return when (config.frequency) {
        "daily" -> true
        "3d" -> sinceLast >= 3
        "weekly" -> last.isBefore(anchor)
        "2w" -> last.isBefore(anchor) && sinceLast >= 13
        else -> false
    }
}

/** Überschrift des Pop-ups, z. B. „MONTAG · WÖCHENTLICHE ABFRAGE“. */
fun weighEyebrow(config: WeighConfig, today: LocalDate): String {
    val day = WEEKDAY_LONG[today.dayOfWeek.value - 1].uppercase()
    val kind = when (config.frequency) {
        "daily" -> "TÄGLICHE ABFRAGE"
        "3d" -> "ABFRAGE ALLE 3 TAGE"
        "2w" -> "ABFRAGE ALLE 2 WOCHEN"
        else -> "WÖCHENTLICHE ABFRAGE"
    }
    return "$day · $kind"
}

fun weighReminderText(config: WeighConfig): String = when (config.frequency) {
    "off" -> "aus"
    "daily" -> "täglich"
    "3d" -> "alle 3 Tage"
    "weekly" -> "wöchentlich, " + WEEKDAY_LONG[config.day].lowercase() + "s"
    else -> "alle 2 Wochen, " + WEEKDAY_LONG[config.day].lowercase() + "s"
}

/** Veränderung pro Woche aus dem geglätteten Trend der letzten 8 Wochen (null bei zu wenig Daten). */
fun weeklyChange(weights: List<WeightEntry>, today: LocalDate): Double? {
    val recent = weights.map { dayOfTimestamp(it.timestamp) to it.kg }.filter { !it.first.isBefore(today.minusDays(56)) }
    if (recent.size < 2) return null
    val trend = weightTrend(recent)
    val span = ChronoUnit.DAYS.between(trend.first().day, trend.last().day)
    if (span < 7) return null
    return (trend.last().trend - trend.first().trend) / span * 7.0
}
