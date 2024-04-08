package xyz.jordanplayz158.ptd.server.module.ptd1

import kotlin.test.Test
import kotlin.test.assertEquals

class KeygenTest {
    @Test
    fun testKeygenInvalidCurrentSave() {
        // Current Save needs to be 14 characters, or it will fail
        assertEquals("invalidCurrentSaveOrTrainerId", PTD1Keygen.generateProfileId("Not14Characters", 333))
        assertEquals("invalidCurrentSaveOrTrainerId", PTD1Keygen.generateProfileId("12Characters", 333))

    }

    @Test
    fun testKeygenInvalidTrainerId() {
        // Trainer ID must be between 333 and 99999, or it will fail
        assertEquals("invalidCurrentSaveOrTrainerId", PTD1Keygen.generateProfileId("Is14Characters", 332))
        assertEquals("invalidCurrentSaveOrTrainerId", PTD1Keygen.generateProfileId("Is14Characters", 100000))
    }

    @Test
    fun testKeygenInvalidCurrentSaveAndTrainerId() {
        // Current Save needs to be 14 characters, or it will fail
        // Trainer ID must be between 333 and 99999, or it will fail
        assertEquals("invalidCurrentSaveOrTrainerId", PTD1Keygen.generateProfileId("Not14Characters", 332))
        assertEquals("invalidCurrentSaveOrTrainerId", PTD1Keygen.generateProfileId("Not14Characters", 100000))
    }

    @Test
    fun testKeygen() {
        assertEquals("ikkg", PTD1Keygen.generateProfileId("10000000000000", 333))
    }
}