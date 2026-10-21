package com.vibecheck.lifepulse.core

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class CurrencyTest {
    @Test fun currentCatalogIncludesWorldwideCurrenciesAndExcludesNonMoneyCodes() {
        assertTrue(CurrencyCatalog.entries.size >= 150)
        listOf("JPY", "AED", "SGD", "CNY", "KWD", "BHD", "NGN", "BRL", "ZWG", "XCG").forEach {
            assertNotNull(it, CurrencyCatalog.find(it))
        }
        assertNull(CurrencyCatalog.find("XXX"))
        assertNull(CurrencyCatalog.find("XAU"))
        assertEquals(CurrencyCatalog.entries.size, CurrencyCatalog.entries.map { it.code }.toSet().size)
    }

    @Test fun localeSuggestsCurrencyWithoutInventingAnIndianFallback() {
        assertEquals("JPY", Money.suggestedCurrency(Locale.JAPAN))
        assertEquals("AED", Money.suggestedCurrency(Locale.forLanguageTag("ar-AE")))
        assertEquals("SGD", Money.suggestedCurrency(Locale.forLanguageTag("en-SG")))
        assertNull(Money.suggestedCurrency(Locale.ENGLISH))
    }

    @Test fun minorUnitsFollowCurrencyPrecisionWithoutRounding() {
        assertEquals(123L, Money.parse("123", "JPY", Locale.US))
        assertNull(Money.parse("123.1", "JPY", Locale.US))
        assertEquals(12345L, Money.parse("12.345", "KWD", Locale.US))
        assertNull(Money.parse("12.3456", "KWD", Locale.US))
        assertEquals(123456L, Money.parse("12.3456", "CLF", Locale.US))
        assertEquals("123", Money.input(123, "JPY", Locale.US))
        assertEquals("12.345", Money.decimal(12345, "KWD"))
    }

    @Test fun entryAcceptsLocalDecimalSeparatorsAndDigitsButRejectsGroupingAndAmbiguity() {
        assertEquals(12345L, Money.parse("123,45", "EUR", Locale.GERMANY))
        assertEquals(12345L, Money.parse("123.45", "EUR", Locale.GERMANY))
        assertEquals(12345L, Money.parse("١٢٣٫٤٥", "AED", Locale.forLanguageTag("ar-AE")))
        assertNull(Money.parse("1.234,56", "EUR", Locale.GERMANY))
        assertNull(Money.parse("1,234.56", "USD", Locale.US))
        assertEquals(-12345L, Money.parse("-12,345", "KWD", Locale.GERMANY, allowNegative = true))
        assertNull(Money.parse("-12", "JPY", Locale.JAPAN))
        assertTrue(Money.acceptsInput("-", allowNegative = true, code = "JPY"))
    }

    @Test fun everySupportedCurrencyRoundTripsAndFormatsAtItsOwnScale() {
        CurrencyCatalog.entries.forEach { currency ->
            val input = Money.input(123456, currency.code, Locale.US)
            assertEquals(currency.code, 123456L, Money.parse(input, currency.code, Locale.US))
            assertTrue(currency.code, Money.format(123456, currency.code, Locale.US).isNotBlank())
        }
        assertFalse(Money.format(123, "JPY", Locale.US).contains(".00"))
        assertTrue(Money.format(12345, "KWD", Locale.US).contains("12.345"))
    }
}
