package com.pntapps.ekchhoan.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class FamilySerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val family = listOf(
        FamilyMember(id = "a1", name = "ছেলে", phone = "+8801712345678", photoPath = "/data/user/0/x/files/photos/a1.jpg", order = 0),
        FamilyMember(id = "b2", name = "Rahim", phone = "০১৭১২৩৪৫৬৭৮", photoPath = null, order = 1),
    )

    @Test
    fun roundTripKeepsEveryField() {
        val raw = json.encodeToString(family)
        assertEquals(family, json.decodeFromString<List<FamilyMember>>(raw))
    }

    @Test
    fun emptyListRoundTrips() {
        assertEquals(emptyList<FamilyMember>(), json.decodeFromString<List<FamilyMember>>(json.encodeToString(emptyList<FamilyMember>())))
    }

    @Test
    fun unknownKeysAreIgnored() {
        val raw = """[{"id":"x","name":"মা","phone":"123","photoPath":null,"order":0,"future":true}]"""
        assertEquals(listOf(FamilyMember("x", "মা", "123", null, 0)), json.decodeFromString<List<FamilyMember>>(raw))
    }

    @Test
    fun contactsSavedBeforeGroupsBelongToFamily() {
        val raw = """[{"id":"x","name":"মা","phone":"123","photoPath":null,"order":0}]"""
        assertEquals(FAMILY_GROUP_ID, json.decodeFromString<List<FamilyMember>>(raw).single().groupId)
    }

    @Test
    fun groupIdRoundTrips() {
        val friend = FamilyMember("f", "Karim", "456", null, 0, groupId = "friends-1")
        assertEquals(friend, json.decodeFromString<FamilyMember>(json.encodeToString(friend)))
    }

    @Test
    fun membersOfFiltersByGroup() {
        val settings = AppSettings(
            family = family + FamilyMember("f", "Karim", "456", null, 2, groupId = "g"),
            groups = listOf(ContactGroup("g", "Friends")),
        )
        assertEquals(listOf("a1", "b2"), settings.membersOf(FAMILY_GROUP_ID).map { it.id })
        assertEquals(listOf("f"), settings.membersOf("g").map { it.id })
    }
}
