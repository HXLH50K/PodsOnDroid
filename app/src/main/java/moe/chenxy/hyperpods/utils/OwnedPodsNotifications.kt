package moe.chenxy.hyperpods.utils

/** Bluetooth hosts also post Xiaomi and other modules' entries; a legacy tag is not ownership. */
internal class OwnedPodsNotifications(private val owner: String) {
    fun isNamespaced(key: PodsNotificationKey): Boolean =
        (key.id == 10004 && key.tag.startsWith("$owner:audio:") &&
            key.tag.length > "$owner:audio:".length) ||
            (key.id == 10003 && key.tag.startsWith("$owner:status:") &&
                key.tag.length > "$owner:status:".length)

    fun isLegacyCandidate(key: PodsNotificationKey): Boolean =
        (key.id == 10003 && key.tag.startsWith("BTHeadset") && key.tag.length > "BTHeadset".length) ||
            (key.id == 1001 && key.tag == "HEADSET_CONNECT_NOTIFICATION")

    fun canRetire(key: PodsNotificationKey, targetPackage: String? = null,
        pendingIntentPackages: Set<String> = emptySet()): Boolean {
        if (!targetPackage.isNullOrEmpty() && targetPackage != owner) return false
        return isNamespaced(key) || isLegacyCandidate(key) &&
            (targetPackage == owner || owner in pendingIntentPackages)
    }
}
