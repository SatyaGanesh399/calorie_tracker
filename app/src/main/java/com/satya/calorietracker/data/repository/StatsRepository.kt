package com.satya.calorietracker.data.repository

import com.satya.calorietracker.data.db.DailyTotalsRow
import com.satya.calorietracker.util.DateUtils
import java.time.LocalDate

enum class StatsRange(val label: String, val days: Int) {
    WEEK("7 days", 7),
    MONTH("30 days", 30),
    QUARTER("3 months", 90),
    HALF("6 months", 182),
    YEAR("1 year", 365),
    ALL("All time", Int.MAX_VALUE)
}

data class DayPoint(val date: LocalDate, val value: Double)

data class CalorieStats(
    val dailyAverage: Double = 0.0,
    val weeklyAverage: Double = 0.0,
    val monthlyAverage: Double = 0.0,
    val highest: DayPoint? = null,
    val lowest: DayPoint? = null,
    val daysTracked: Int = 0,
    val daysInRange: Int = 0,
    val series: List<DayPoint> = emptyList()
)

data class MacroStats(
    val avgProtein: Double = 0.0,
    val avgCarbs: Double = 0.0,
    val avgFat: Double = 0.0,
    val avgFiber: Double = 0.0,
    val avgSugar: Double = 0.0,
    val avgSodium: Double = 0.0
)

/**
 * Read-only analytics over the diary. Everything is computed from the same
 * `GROUP BY date` query so a year of history is one round-trip, not 365.
 */
class StatsRepository(
    private val diaryRepository: DiaryRepository,
    private val waterRepository: WaterRepository
) {

    suspend fun rangeStart(range: StatsRange): LocalDate = when (range) {
        StatsRange.ALL -> diaryRepository.earliestLoggedDate() ?: DateUtils.today()
        else -> DateUtils.today().minusDays((range.days - 1).toLong())
    }

    suspend fun calorieStats(range: StatsRange): CalorieStats {
        val start = rangeStart(range)
        val end = DateUtils.today()
        val rows = diaryRepository.totalsBetween(start, end).filter { it.entryCount > 0 }
        if (rows.isEmpty()) return CalorieStats(daysInRange = daysBetween(start, end))

        val series = rows.map { DayPoint(DateUtils.parse(it.date), it.calories) }
        val avg = series.sumOf { it.value } / series.size

        return CalorieStats(
            dailyAverage = avg,
            weeklyAverage = avg * 7,
            monthlyAverage = avg * 30,
            highest = series.maxByOrNull { it.value },
            lowest = series.minByOrNull { it.value },
            daysTracked = series.size,
            daysInRange = daysBetween(start, end),
            series = series
        )
    }

    suspend fun macroStats(range: StatsRange): MacroStats {
        val rows = trackedRows(range)
        if (rows.isEmpty()) return MacroStats()
        val n = rows.size.toDouble()
        return MacroStats(
            avgProtein = rows.sumOf { it.protein } / n,
            avgCarbs = rows.sumOf { it.carbs } / n,
            avgFat = rows.sumOf { it.fat } / n,
            avgFiber = rows.sumOf { it.fiber } / n,
            avgSugar = rows.sumOf { it.sugar } / n,
            avgSodium = rows.sumOf { it.sodium } / n
        )
    }

    /** Water totals per day over a range. */
    suspend fun waterTotals(range: StatsRange): List<DayPoint> {
        val start = rangeStart(range)
        val end = DateUtils.today()
        return (0..daysBetween(start, end)).mapNotNull { offset ->
            val day = start.plusDays(offset.toLong())
            val total = waterRepository.totalFor(day)
            if (total > 0) DayPoint(day, total) else null
        }
    }

    /** Consecutive days with at least one entry, counting back from today. */
    suspend fun currentStreak(): Int {
        val start = DateUtils.today().minusDays(400)
        val logged = diaryRepository.totalsBetween(start, DateUtils.today())
            .filter { it.entryCount > 0 }
            .mapTo(mutableSetOf()) { it.date }

        var streak = 0
        var day = DateUtils.today()
        // Today not being logged yet shouldn't break yesterday's streak.
        if (DateUtils.iso(day) !in logged) day = day.minusDays(1)
        while (DateUtils.iso(day) in logged) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    private suspend fun trackedRows(range: StatsRange): List<DailyTotalsRow> {
        val start = rangeStart(range)
        return diaryRepository.totalsBetween(start, DateUtils.today()).filter { it.entryCount > 0 }
    }

    private fun daysBetween(start: LocalDate, end: LocalDate): Int =
        (java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt() + 1).coerceAtLeast(1)
}
