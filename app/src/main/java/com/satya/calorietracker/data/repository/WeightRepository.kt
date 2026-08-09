package com.satya.calorietracker.data.repository

import com.satya.calorietracker.data.db.WeightDao
import com.satya.calorietracker.data.db.WeightEntryEntity
import com.satya.calorietracker.data.prefs.PreferencesRepository
import com.satya.calorietracker.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** A single weigh-in, in kilograms. The UI converts for display. */
data class WeightEntry(
    val id: Long = 0L,
    val date: LocalDate,
    val timestamp: Long,
    val weightKg: Double,
    val notes: String? = null
)

/** Everything the Progress screen needs about weight, precomputed. */
data class WeightSummary(
    val current: Double? = null,
    val start: Double? = null,
    val goal: Double? = null,
    val weeklyChange: Double? = null,
    val monthlyChange: Double? = null,
    val totalChange: Double? = null,
    val remainingToGoal: Double? = null,
    val entryCount: Int = 0
)

class WeightRepository(
    private val weightDao: WeightDao,
    private val prefs: PreferencesRepository,
    private val notifier: DataChangeNotifier = DataChangeNotifier.NONE
) {

    fun observeAll(): Flow<List<WeightEntry>> =
        weightDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<WeightEntry>> =
        weightDao.observeBetween(DateUtils.iso(start), DateUtils.iso(end))
            .map { list -> list.map { it.toDomain() } }

    fun observeLatest(): Flow<WeightEntry?> =
        weightDao.observeLatest().map { it?.toDomain() }

    suspend fun latest(): WeightEntry? = weightDao.getLatest()?.toDomain()

    suspend fun forDate(date: LocalDate): WeightEntry? =
        weightDao.getForDate(DateUtils.iso(date))?.toDomain()

    /**
     * One weigh-in per day: logging twice on the same date replaces the earlier value
     * rather than creating a second point that would make the chart jagged.
     */
    suspend fun log(weightKg: Double, date: LocalDate = DateUtils.today(), notes: String? = null): Long {
        val iso = DateUtils.iso(date)
        val existing = weightDao.getForDate(iso)
        val entity = WeightEntryEntity(
            id = existing?.id ?: 0L,
            date = iso,
            timestamp = DateUtils.nowMillis(),
            weightKg = weightKg,
            notes = notes
        )
        val id = weightDao.insert(entity)

        prefs.setStartWeightIfAbsent(weightKg)
        // Keep the profile in step so the calorie calculator uses today's weight.
        if (date == DateUtils.today()) prefs.setCurrentWeight(weightKg)

        notifier.onDataChanged()
        return id
    }

    suspend fun delete(id: Long) {
        weightDao.deleteById(id)
        notifier.onDataChanged()
    }

    /** Change over the last [days] days, negative means lost. */
    suspend fun changeOverDays(days: Long): Double? {
        val latest = weightDao.getLatest() ?: return null
        val past = weightDao.getOnOrBefore(DateUtils.iso(DateUtils.today().minusDays(days)))
            ?: return null
        if (past.id == latest.id) return null
        return latest.weightKg - past.weightKg
    }

    suspend fun summary(goalKg: Double?): WeightSummary {
        val all = weightDao.getAll()
        if (all.isEmpty()) return WeightSummary(goal = goalKg)

        val first = all.first()
        val last = all.last()
        return WeightSummary(
            current = last.weightKg,
            start = first.weightKg,
            goal = goalKg,
            weeklyChange = changeOverDays(7),
            monthlyChange = changeOverDays(30),
            totalChange = last.weightKg - first.weightKg,
            remainingToGoal = goalKg?.let { last.weightKg - it },
            entryCount = all.size
        )
    }

    suspend fun exportAll(): List<WeightEntry> = weightDao.getAll().map { it.toDomain() }

    suspend fun importAll(entries: List<WeightEntry>) {
        weightDao.insertAll(entries.map { it.toEntity() })
        notifier.onDataChanged()
    }

    private fun WeightEntryEntity.toDomain() = WeightEntry(
        id = id,
        date = DateUtils.parse(date),
        timestamp = timestamp,
        weightKg = weightKg,
        notes = notes
    )

    private fun WeightEntry.toEntity() = WeightEntryEntity(
        id = id,
        date = DateUtils.iso(date),
        timestamp = timestamp,
        weightKg = weightKg,
        notes = notes
    )
}
