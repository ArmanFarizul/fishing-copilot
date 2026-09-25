package com.fishingcopilot.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fishingcopilot.catchlog.CatchConditions
import com.fishingcopilot.catchlog.LogPeriod
import com.fishingcopilot.catchlog.LogSummary
import com.fishingcopilot.catchlog.MonthGroup
import com.fishingcopilot.catchlog.PhotoStore
import com.fishingcopilot.catchlog.edited
import com.fishingcopilot.catchlog.groupByMonth
import com.fishingcopilot.data.local.CatchDao
import com.fishingcopilot.data.local.CatchLogEntity
import com.fishingcopilot.data.local.FishingDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class LoggedCatch(val entity: CatchLogEntity, val spotName: String?)

data class LogUiState(
    /** False until the first read from the database, so the screen never flashes "no catches". */
    val loaded: Boolean = false,
    val period: LogPeriod = LogPeriod.ThisYear,
    /** Catches in [period], newest first. */
    val catches: List<LoggedCatch> = emptyList(),
    /** The same catches grouped by month, for the list's month headers. */
    val months: List<MonthGroup<LoggedCatch>> = emptyList(),
    /** Summary of [period] only. */
    val summary: LogSummary? = null,
    /** Years that have catches, for the year picker. */
    val years: List<Int> = emptyList(),
    /** Whether the log has any catch at all, so an empty period can offer "all catches". */
    val hasAnyCatch: Boolean = false,
    val recentBaits: List<String> = emptyList()
)

class LogViewModel(
    private val catches: CatchDao,
    spots: FishingDao,
    private val photos: PhotoStore,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val today: () -> LocalDate = { LocalDate.now(zone()) }
) : ViewModel() {
    // Starts on this year: after a few seasons "all" mixes old patterns into the summary.
    private val period = MutableStateFlow<LogPeriod>(LogPeriod.ThisYear)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val periodCatches: Flow<List<CatchLogEntity>> = period.flatMapLatest { p ->
        val range = p.bounds(today(), zone())
        if (range == null) catches.allFlow() else catches.betweenFlow(range.first, range.last + 1)
    }

    val uiState: StateFlow<LogUiState> = combine(
        combine(period, periodCatches, ::Pair),
        spots.getAllSpotsFlow(),
        catches.yearsFlow(),
        catches.recentBaitsFlow(RECENT_BAITS)
    ) { (period, logs), spots, years, baits ->
        val names = spots.associate { it.id to it.name }
        val logged = logs.map { LoggedCatch(it, it.spotId?.let(names::get)) }
        LogUiState(
            loaded = true,
            period = period,
            catches = logged,
            months = groupByMonth(logged, zone()) { it.entity.timestamp },
            summary = LogSummary.of(logs),
            years = years,
            hasAnyCatch = years.isNotEmpty(),
            recentBaits = baits
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LogUiState())

    fun show(next: LogPeriod) {
        // Picking this or last year from the list lights up its own chip instead of a duplicate "2026".
        val year = today().year
        period.value = when (next) {
            LogPeriod.Year(year) -> LogPeriod.ThisYear
            LogPeriod.Year(year - 1) -> LogPeriod.LastYear
            else -> next
        }
    }

    fun save(
        conditions: CatchConditions,
        species: String,
        bait: String?,
        weightKg: Double?,
        lengthCm: Double?,
        pickedPhoto: String?,
        notes: String?,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val photo = pickedPhoto?.let { photos.import(it) }
            catches.insert(conditions.toEntity(species, bait, weightKg, lengthCm, photo, notes))
            onSaved()
        }
    }

    /**
     * Saves changed details. [pickedPhoto] equal to the stored photo keeps it; a new pick is copied in
     * and the old copy removed; null removes the photo.
     */
    fun update(
        original: CatchLogEntity,
        species: String,
        bait: String?,
        weightKg: Double?,
        lengthCm: Double?,
        pickedPhoto: String?,
        notes: String?,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val photo = if (pickedPhoto == original.photoUri) pickedPhoto else pickedPhoto?.let { photos.import(it) }
            catches.update(original.edited(species, bait, weightKg, lengthCm, photo, notes))
            // Only drop the old file once the row no longer points at it.
            original.photoUri?.takeIf { it != photo }?.let { photos.delete(it) }
            onSaved()
        }
    }

    fun delete(log: CatchLogEntity) {
        viewModelScope.launch {
            catches.delete(log)
            log.photoUri?.let { photos.delete(it) }
        }
    }

    companion object {
        const val RECENT_BAITS = 6

        fun factory(catches: CatchDao, spots: FishingDao, photos: PhotoStore) = viewModelFactory {
            initializer { LogViewModel(catches, spots, photos) }
        }
    }
}
