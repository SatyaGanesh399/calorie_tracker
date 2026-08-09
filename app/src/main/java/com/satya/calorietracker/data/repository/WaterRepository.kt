package com.satya.calorietracker.data.repository

import com.satya.calorietracker.data.db.DateAmountRow
import com.satya.calorietracker.data.db.WaterDao
import com.satya.calorietracker.data.db.WaterEntryEntity
import com.satya.calorietracker.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

data class WaterEntry(
    val id: Long = 0L,
    val date: LocalDate,
    val timestamp: Long,
    val amountMl: Double
)

class WaterRepository(
    private val waterDao: WaterDao,
    private val notifier: DataChangeNotifier = DataChangeNotifier.NONE
) {

    fun observeTotal(date: LocalDate): Flow<Double> =
        waterDao.observeTotalForDate(DateUtils.iso(date))

    fun observeEntries(date: LocalDate): Flow<List<WaterEntry>> =
        waterDao.observeForDate(DateUtils.iso(date)).map { list -> list.map { it.toDomain() } }

    fun observeTotalsBetween(start: LocalDate, end: LocalDate): Flow<List<DateAmountRow>> =
        waterDao.observeTotalsBetween(DateUtils.iso(start), DateUtils.iso(end))

    suspend fun totalFor(date: LocalDate): Double = waterDao.getTotalForDate(DateUtils.iso(date))

    suspend fun add(amountMl: Double, date: LocalDate = DateUtils.today()): Long {
        val id = waterDao.insert(
            WaterEntryEntity(
                date = DateUtils.iso(date),
                timestamp = DateUtils.nowMillis(),
                amountMl = amountMl
            )
        )
        notifier.onDataChanged()
        return id
    }

    /** Undo the most recent glass for a day. */
    suspend fun undoLast(date: LocalDate = DateUtils.today()) {
        waterDao.deleteLastForDate(DateUtils.iso(date))
        notifier.onDataChanged()
    }

    suspend fun delete(id: Long) {
        waterDao.deleteById(id)
        notifier.onDataChanged()
    }

    suspend fun exportAll(): List<WaterEntry> = waterDao.getAll().map { it.toDomain() }

    suspend fun importAll(entries: List<WaterEntry>) {
        waterDao.insertAll(entries.map { it.toEntity() })
        notifier.onDataChanged()
    }

    private fun WaterEntryEntity.toDomain() = WaterEntry(
        id = id,
        date = DateUtils.parse(date),
        timestamp = timestamp,
        amountMl = amountMl
    )

    private fun WaterEntry.toEntity() = WaterEntryEntity(
        id = id,
        date = DateUtils.iso(date),
        timestamp = timestamp,
        amountMl = amountMl
    )
}
