package xyz.jordanplayz158.ptd1.server

import xyz.jordanplayz158.ptd.module.ptd1.Keygen
import kotlin.test.Test
import kotlin.test.assertEquals

class KeygenTest {
    @Test
    fun testKeygenInvalidCurrentSave() {
        // Current Save needs to be 14 characters, or it will fail
        assertEquals("invalidCurrentSaveOrTrainerId", Keygen.generateProfileId("Not14Characters", 333))
        assertEquals("invalidCurrentSaveOrTrainerId", Keygen.generateProfileId("12Characters", 333))

    }

    @Test
    fun testKeygenInvalidTrainerId() {
        // Trainer ID must be between 333 and 99999, or it will fail
        assertEquals("invalidCurrentSaveOrTrainerId", Keygen.generateProfileId("Is14Characters", 332))
        assertEquals("invalidCurrentSaveOrTrainerId", Keygen.generateProfileId("Is14Characters", 100000))
    }

    @Test
    fun testKeygenInvalidCurrentSaveAndTrainerId() {
        // Current Save needs to be 14 characters, or it will fail
        // Trainer ID must be between 333 and 99999, or it will fail
        assertEquals("invalidCurrentSaveOrTrainerId", Keygen.generateProfileId("Not14Characters", 332))
        assertEquals("invalidCurrentSaveOrTrainerId", Keygen.generateProfileId("Not14Characters", 100000))
    }

    @Test
    fun testKeygen() {
        assertEquals("ikkg", Keygen.generateProfileId("10000000000000", 333))
    }
}