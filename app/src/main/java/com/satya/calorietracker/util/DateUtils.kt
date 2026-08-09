package com.satya.calorietracker.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Dates are stored as ISO `yyyy-MM-dd` strings so they sort lexicographically in SQL
 * and never suffer time-zone drift when the phone travels.
 */
object DateUtils {

    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val PRETTY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM")
    private val SHORT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
    private val MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val DAY_LETTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE")

    fun today(): LocalDate = LocalDate.now()

    fun todayIso(): String = today().format(ISO)

    fun iso(date: LocalDate): String = date.format(ISO)

    fun parse(iso: String): LocalDate = runCatching { LocalDate.parse(iso, ISO) }.getOrElse { today() }

    fun nowMillis(): Long = System.currentTimeMillis()

    fun millisToTimeLabel(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime().format(TIME)

    fun millisToLocalDateTime(millis: Long): LocalDateTime =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()

    fun toMillis(date: LocalDate, time: LocalTime): Long =
        date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun startOfDayMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** Millis until the next local midnight — used to arm the daily-reset alarm. */
    fun millisUntilNextMidnight(): Long {
        val now = LocalDateTime.now()
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
        return ChronoUnit.MILLIS.between(now, midnight).coerceAtLeast(1000L)
    }

    fun prettyDate(date: LocalDate): String = when (date) {
        today() -> "Today"
        today().minusDays(1) -> "Yesterday"
        today().plusDays(1) -> "Tomorrow"
        else -> date.format(PRETTY)
    }

    fun fullDate(date: LocalDate): String = date.format(PRETTY)
    fun shortDate(date: LocalDate): String = date.format(SHORT)
    fun monthYear(date: LocalDate): String = date.format(MONTH_YEAR)
    fun dayLetter(date: LocalDate): String = date.format(DAY_LETTER)

    /** The 7 dates of the week containing [date], Monday first. */
    fun weekOf(date: LocalDate): List<LocalDate> {
        val monday = date.minusDays(((date.dayOfWeek.value + 6) % 7).toLong())
        return (0..6).map { monday.plusDays(it.toLong()) }
    }

    fun lastNDays(n: Int, endingAt: LocalDate = today()): List<LocalDate> =
        (n - 1 downTo 0).map { endingAt.minusDays(it.toLong()) }

    fun greeting(hour: Int = LocalTime.now().hour): String = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
}
