package moe.chenxy.hyperpods.utils

import moe.chenxy.hyperpods.pods.BatteryComponent
import moe.chenxy.hyperpods.pods.BatteryStatus
import moe.chenxy.hyperpods.utils.data.BatteryParams
import moe.chenxy.hyperpods.utils.data.PodBatteryParams

/** One real A2DP session. The caller owns identity, report ordering and presentation. */
class BatteryNotificationSession(
    settings: BatteryNotificationSettings = BatteryNotificationSettings(),
    suppressAll: Boolean = false,
) {
    enum class Category { LOW, MEDIUM }

    enum class Budget {
        UNINITIALIZED, ARMED, SUPPRESSED_INITIAL, SUPPRESSED_DISABLED, CONSUMED, SUPPRESSED_RESUMED;

        val terminal: Boolean get() = this != UNINITIALIZED && this != ARMED
    }

    data class Reason(val component: Int, val category: Category)

    data class EarSnapshot(
        val level: Int?,
        val charging: Boolean?,
        val low: Budget,
        val medium: Budget,
    )

    data class Snapshot(
        val settings: BatteryNotificationSettings,
        val settingsRevision: Long,
        val left: EarSnapshot,
        val right: EarSnapshot,
    )

    private class EarState(suppressAll: Boolean) {
        var level: Int? = null
        var charging: Boolean? = null
        var low = if (suppressAll) Budget.SUPPRESSED_RESUMED else Budget.UNINITIALIZED
        var medium = low

        fun snapshot() = EarSnapshot(level, charging, low, medium)

        fun clearReading() {
            level = null
            charging = null
        }
    }

    init {
        require(settings.valid) { "Battery thresholds must satisfy 1 <= low < medium <= 99" }
    }

    private var configuration = settings
    private var revision = 0L
    private val leftState = EarState(suppressAll)
    private val rightState = EarState(suppressAll)

    val left: EarSnapshot
        @Synchronized get() = leftState.snapshot()

    val right: EarSnapshot
        @Synchronized get() = rightState.snapshot()

    @Synchronized
    fun snapshot() = Snapshot(configuration, revision, leftState.snapshot(), rightState.snapshot())

    /**
     * Only listed ears advance, including unavailable reports; an empty fresh list is a no-op.
     * Returned reasons form one event, ordered LOW before MEDIUM for each ear.
     * CONSUMED means reserved, not displayed: posting failure must not restore its budget.
     */
    @Synchronized
    fun observe(params: BatteryParams, fresh: IntArray): Set<Reason> {
        val reasons = linkedSetOf<Reason>()
        if (BatteryComponent.LEFT in fresh) {
            observeEar(leftState, BatteryComponent.LEFT, params.left, reasons)
        }
        if (BatteryComponent.RIGHT in fresh) {
            observeEar(rightState, BatteryComponent.RIGHT, params.right, reasons)
        }
        return reasons.toSet()
    }

    /** Returns false for an invalid pair; true accepts it, without notifying or reopening budgets. */
    @Synchronized
    fun settingsChanged(new: BatteryNotificationSettings): Boolean {
        if (!new.valid) return false
        if (new == configuration) return true
        configuration = new
        revision++
        rebaseline(leftState)
        rebaseline(rightState)
        return true
    }

    /** Discard stale display/baselines, but never forgive a consumed or suppressed category. */
    @Synchronized
    fun controlGap() {
        leftState.clearReading()
        rightState.clearReading()
    }

    /** Fail closed when audio was already connected and session continuity cannot be proven. */
    @Synchronized
    fun resumedUnknown() {
        suppressResumed(leftState)
        suppressResumed(rightState)
    }

    private fun suppressResumed(ear: EarState) {
        ear.clearReading()
        if (!ear.low.terminal) ear.low = Budget.SUPPRESSED_RESUMED
        if (!ear.medium.terminal) ear.medium = Budget.SUPPRESSED_RESUMED
    }

    private fun observeEar(
        ear: EarState,
        component: Int,
        params: PodBatteryParams?,
        reasons: MutableSet<Reason>,
    ) {
        if (params == null || !params.isConnected || params.battery !in 0..100 ||
            (params.rawStatus != BatteryStatus.CHARGING && params.rawStatus != BatteryStatus.NOT_CHARGING)
        ) {
            ear.clearReading()
            return
        }

        val previous = ear.level
        val current = params.battery
        // Conflicting flags fail closed for reminders; either charging indication inhibits them.
        val charging = params.isCharging || params.rawStatus == BatteryStatus.CHARGING
        ear.level = current
        ear.charging = charging
        if (previous == null) {
            rebaseline(ear)
            return
        }
        if (charging) return

        val low = cross(ear.low, previous, current, configuration.lowThreshold, configuration.lowReminderEnabled)
        val medium = cross(
            ear.medium, previous, current, configuration.mediumThreshold, configuration.mediumReminderEnabled,
        )
        val reportLow = low == Budget.CONSUMED && ear.low != Budget.CONSUMED
        val reportMedium = medium == Budget.CONSUMED && ear.medium != Budget.CONSUMED
        // Reserve both categories before exposing any reason to the caller.
        ear.low = low
        ear.medium = medium
        if (reportLow) reasons.add(Reason(component, Category.LOW))
        if (reportMedium) reasons.add(Reason(component, Category.MEDIUM))
    }

    private fun cross(
        budget: Budget,
        previous: Int,
        current: Int,
        threshold: Int,
        enabled: Boolean,
    ): Budget {
        if (budget != Budget.ARMED || previous <= threshold || current > threshold || current >= previous) {
            return budget
        }
        return if (enabled) Budget.CONSUMED else Budget.SUPPRESSED_DISABLED
    }

    private fun rebaseline(ear: EarState) {
        ear.low = baseline(ear.low, ear.level, configuration.lowThreshold)
        ear.medium = baseline(ear.medium, ear.level, configuration.mediumThreshold)
    }

    private fun baseline(budget: Budget, level: Int?, threshold: Int): Budget = when {
        budget.terminal -> budget
        level == null -> Budget.UNINITIALIZED
        level > threshold -> Budget.ARMED
        else -> Budget.SUPPRESSED_INITIAL
    }
}
