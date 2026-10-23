package moe.chenxy.hyperpods.utils

/** One physical stream, serialized by L2CAPController's lock. A call can preempt
 * a preview, but a late preview stop must never stop a call-owned stream. */
class HeadTrackingStream(
    private val sendStandard: ((start: Boolean) -> Boolean)? = null,
    private val send: (start: Boolean) -> Boolean
) {
    enum class Owner { CALL, PREVIEW }
    enum class Variant { ALTERNATE, STANDARD }
    private var owner: Owner? = null
    private var interrupted: (() -> Unit)? = null
    var variant = Variant.ALTERNATE
        private set
    var stopPending = false
        private set

    fun isOwnedBy(request: Owner): Boolean = owner == request

    fun acquire(request: Owner, onInterrupted: () -> Unit = {}): Boolean {
        if (stopPending && !retryStop()) return false
        if (owner == request) return true
        if (owner == Owner.CALL) return false
        if (owner == Owner.PREVIEW) {
            val callback = interrupted
            if (!release(Owner.PREVIEW)) {
                callback?.invoke()
                return false
            }
            callback?.invoke()
        }
        variant = Variant.ALTERNATE
        if (!sendCurrent(true)) return false
        owner = request
        interrupted = onInterrupted
        return true
    }

    fun release(request: Owner): Boolean {
        if (owner != request) return true
        if (stopPending) return false
        return stopOwner()
    }

    private fun sendCurrent(start: Boolean) =
        if (variant == Variant.STANDARD) sendStandard?.invoke(start) == true else send(start)

    private fun stopOwner(): Boolean {
        if (!sendCurrent(false)) {
            stopPending = true
            return false
        }
        owner = null
        interrupted = null
        stopPending = false
        return true
    }

    fun retryStop(): Boolean = if (stopPending && owner != null) stopOwner() else !stopPending

    fun canRetryWithStandard(request: Owner): Boolean =
        owner == request && !stopPending && variant == Variant.ALTERNATE && sendStandard != null

    /** One bounded retry using LibrePods' other existing pair, never overlapping streams. */
    fun retryWithStandard(request: Owner): Boolean {
        if (!canRetryWithStandard(request)) return false
        val callback = interrupted
        if (!release(request)) return false
        variant = Variant.STANDARD
        if (!sendCurrent(true)) return false
        owner = request
        interrupted = callback
        return true
    }

    /** Only after the associated control socket has been closed/discarded. */
    fun reset() {
        owner = null
        interrupted = null
        stopPending = false
        variant = Variant.ALTERNATE
    }
}
