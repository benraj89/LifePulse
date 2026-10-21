package com.vibecheck.lifepulse.core

import com.vibecheck.lifepulse.data.backup.BackupChecksum
import com.vibecheck.lifepulse.data.backup.CsvFormat
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest
import java.util.Locale
import java.util.TimeZone

class BackupFormatTest {
    @Test fun checksumMatchesKnownSha256Vector() {
        val expected = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        assertEquals(expected, BackupChecksum.sha256("abc".byteInputStream()))
        assertTrue(BackupChecksum.verify(expected, MessageDigest.getInstance("SHA-256").digest("abc".toByteArray())))
        assertFalse(BackupChecksum.verify(expected, ByteArray(32)))
        assertFalse(BackupChecksum.verify("g".repeat(64), ByteArray(32)))
        assertFalse(BackupChecksum.verify("00", ByteArray(32)))
    }

    @Test fun csvQuotesCommasQuotesAndNewlines() {
        assertEquals("\"Lunch, \"\"tea\"\"\r\nnext line\"", CsvFormat.cell("Lunch, \"tea\"\r\nnext line"))
        assertEquals("\"\"", CsvFormat.cell(""))
    }

    @Test fun csvNeutralizesSpreadsheetFormulas() {
        listOf("=1+1", "+cmd", "-1+1", "@SUM(A1)", " \t=1+1", "\ttext", "\rtext", "\ntext", "\uFEFF=1").forEach {
            assertTrue(CsvFormat.cell(it).startsWith("\"'"))
        }
        assertEquals("\"12.34\"", CsvFormat.cell("12.34", protectFormula = false))
        assertEquals("\"Alex\"", CsvFormat.cell("Alex"))
    }

    @Test fun datesAreStableAcrossDeviceLocaleAndTimezone() {
        val locale = Locale.getDefault()
        val zone = TimeZone.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
            assertEquals("1970-01-01 00:00:00", CsvFormat.date(0))
        } finally { Locale.setDefault(locale); TimeZone.setDefault(zone) }
    }
}
