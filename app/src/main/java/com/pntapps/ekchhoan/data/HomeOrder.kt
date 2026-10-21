package com.pntapps.ekchhoan.data

/**
 * Keys for everything Home can show, in the order the family arranges in Settings → Home screen order.
 * Favourite apps use "app:<package>".
 */
object HomeItem {
    const val CONTACTS = "contacts"
    const val YOUTUBE = "youtube"
    const val CAMERA = "camera"
    const val GALLERY = "gallery"
    const val TORCH = "torch"
    const val INTERNET = "internet"
    private const val APP_PREFIX = "app:"

    fun app(packageName: String) = APP_PREFIX + packageName
    fun packageOf(key: String): String? = key.removePrefix(APP_PREFIX).takeIf { key.startsWith(APP_PREFIX) }

    /** Full-width items; everything else is a tile in the two-column grid. */
    fun isFullWidth(key: String) = key == CONTACTS || key == INTERNET
}

/**
 * The Home items to show, in display order.
 *
 * Items the family placed in [AppSettings.homeOrder] keep that order. Anything not placed yet follows the
 * default: contacts, then calling and messaging apps ([communicationPackages]: phone, contacts, messages,
 * WhatsApp), then YouTube, Camera, Photos and Torch, then other favourite apps, and the internet bar last.
 */
fun AppSettings.homeItems(communicationPackages: Set<String>, torchAvailable: Boolean = true): List<String> {
    val (talk, other) = favoriteApps.partition { it in communicationPackages }
    val defaults = buildList {
        add(HomeItem.CONTACTS)
        talk.forEach { add(HomeItem.app(it)) }
        if (showYouTube) add(HomeItem.YOUTUBE)
        if (showCamera) add(HomeItem.CAMERA)
        if (showGallery) add(HomeItem.GALLERY)
        if (showTorch && torchAvailable) add(HomeItem.TORCH)
        other.forEach { add(HomeItem.app(it)) }
        if (showInternetBar) add(HomeItem.INTERNET)
    }
    val placed = homeOrder.filter { it in defaults }
    // Unplaced items slot in after the placed item that precedes them by default, so a newly added
    // app or a re-enabled tile appears where it normally would instead of jumping to the end.
    val result = placed.toMutableList()
    defaults.forEachIndexed { i, key ->
        if (key in result) return@forEachIndexed
        val anchor = defaults.subList(0, i).lastOrNull { it in result }
        result.add(if (anchor == null) 0 else result.indexOf(anchor) + 1, key)
    }
    return result
}

/** [items] with [key] moved one place up ([delta] = -1) or down (+1). */
fun moveHomeItem(items: List<String>, key: String, delta: Int): List<String> {
    val i = items.indexOf(key)
    val j = i + delta
    if (i < 0 || j !in items.indices) return items
    return items.toMutableList().apply { add(j, removeAt(i)) }
}
