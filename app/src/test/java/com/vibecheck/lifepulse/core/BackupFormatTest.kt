package com.vibecheck.lifepulse.core

import com.vibecheck.lifepulse.data.backup.BackupChecksum
import com.vibecheck.lifepulse.data.backup.BackupFormat
import com.vibecheck.lifepulse.data.backup.InvalidBackupException
import com.vibecheck.lifepulse.data.backup.CsvFormat
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest
import java.util.Locale
import java.util.TimeZone

class BackupFormatTest {
    @Test fun legacyAmountsPreserveTheirValueInEverySupportedCurrency() {
        CurrencyCatalog.entries.forEach { currency ->
            val minor = BackupFormat.legacyMinorUnits(12300, currency.code)
            assertEquals(currency.code, 0, java.math.BigDecimal("123").compareTo(
                java.math.BigDecimal.valueOf(minor, currency.fractionDigits)))
            assertEquals(-minor, BackupFormat.legacyMinorUnits(-12300, currency.code))
            assertEquals(0L, BackupFormat.legacyMinorUnits(0, currency.code))
        }
        assertEquals(1234L, BackupFormat.legacyMinorUnits(1234, "AED"))
        assertEquals(12340L, BackupFormat.legacyMinorUnits(1234, "KWD"))
        assertEquals(123400L, BackupFormat.legacyMinorUnits(1234, "CLF"))
    }

    @Test fun legacyAmountsRejectRoundingAndOverflowInsteadOfChangingValue() {
        listOf(1234L to "JPY", -1234L to "JPY", Long.MAX_VALUE to "KWD",
            Long.MIN_VALUE to "CLF", 100L to "XXX").forEach { (amount, code) ->
            try {
                BackupFormat.legacyMinorUnits(amount, code)
                fail("Expected rejection for $amount $code")
            } catch (_: InvalidBackupException) { }
        }
    }

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
