package moe.chenxy.hyperpods.utils

internal object ListeningCycleCommit {
    enum class Action { CLOSE, SUBMIT, INVALID, UNAVAILABLE }

    fun evaluate(initial: Int, selected: Int, allowed: Int, enabled: Boolean, open: Boolean): Action = when {
        !open || selected == initial -> Action.CLOSE
        !enabled -> Action.UNAVAILABLE
        !valid(selected, allowed) -> Action.INVALID
        else -> Action.SUBMIT
    }

    fun valid(selected: Int, allowed: Int): Boolean =
        allowed in 0..15 && selected in 1..15 && selected and allowed == selected && Integer.bitCount(selected) >= 2
}
