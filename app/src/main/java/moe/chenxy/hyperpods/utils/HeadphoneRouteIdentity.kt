package moe.chenxy.hyperpods.utils

internal object HeadphoneRouteIdentity {
    private val address = Regex("(?:[0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}")

    fun systemAddress(routeId: String, systemRoute: Boolean): String? {
        if (!systemRoute || routeId.length < 17) return null
        val suffix = routeId.takeLast(17)
        return suffix.takeIf {
            address.matches(it) && (routeId.length == 17 || routeId[routeId.length - 18] == ':')
        }
    }
}
