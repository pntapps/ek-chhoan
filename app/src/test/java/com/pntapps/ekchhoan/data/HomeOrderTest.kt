package com.pntapps.ekchhoan.data

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeOrderTest {
    private val phone = "com.google.android.dialer"
    private val messages = "com.google.android.apps.messaging"
    private val whatsApp = "com.whatsapp"
    private val chrome = "com.android.chrome"
    private val talk = setOf(phone, messages, whatsApp)

    @Test
    fun defaultPutsCallingAppsRightBelowContacts() {
        val s = AppSettings(favoriteApps = listOf(chrome, phone, messages, whatsApp))
        assertEquals(
            listOf(
                HomeItem.CONTACTS,
                HomeItem.app(phone), HomeItem.app(messages), HomeItem.app(whatsApp),
                HomeItem.YOUTUBE, HomeItem.CAMERA, HomeItem.GALLERY, HomeItem.TORCH,
                HomeItem.app(chrome),
                HomeItem.INTERNET,
            ),
            s.homeItems(talk),
        )
    }

    @Test
    fun hiddenTilesAndMissingTorchAreLeftOut() {
        val s = AppSettings(showCamera = false, showInternetBar = false)
        assertEquals(
            listOf(HomeItem.CONTACTS, HomeItem.YOUTUBE, HomeItem.GALLERY),
            s.homeItems(talk, torchAvailable = false),
        )
    }

    @Test
    fun savedOrderWinsAndNewItemsSlotInAtTheirDefaultPlace() {
        val s = AppSettings(
            favoriteApps = listOf(phone),
            homeOrder = listOf(HomeItem.TORCH, HomeItem.CONTACTS, HomeItem.app(phone), HomeItem.INTERNET),
        )
        // YouTube, Camera and Photos were never placed: they follow the phone app, as in the default order.
        assertEquals(
            listOf(
                HomeItem.TORCH, HomeItem.CONTACTS, HomeItem.app(phone),
                HomeItem.YOUTUBE, HomeItem.CAMERA, HomeItem.GALLERY, HomeItem.INTERNET,
            ),
            s.homeItems(talk),
        )
    }

    @Test
    fun removedAppsDropOutOfTheSavedOrder() {
        val s = AppSettings(homeOrder = listOf(HomeItem.app(chrome), HomeItem.CONTACTS))
        assertEquals(HomeItem.CONTACTS, s.homeItems(talk).first())
    }

    @Test
    fun moveSwapsWithNeighbourAndStopsAtTheEnds() {
        val items = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), moveHomeItem(items, "b", -1))
        assertEquals(listOf("a", "c", "b"), moveHomeItem(items, "b", +1))
        assertEquals(items, moveHomeItem(items, "a", -1))
        assertEquals(items, moveHomeItem(items, "c", +1))
    }

    @Test
    fun appKeysRoundTrip() {
        assertEquals(chrome, HomeItem.packageOf(HomeItem.app(chrome)))
        assertEquals(null, HomeItem.packageOf(HomeItem.CAMERA))
    }
}
