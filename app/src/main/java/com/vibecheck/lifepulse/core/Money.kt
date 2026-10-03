package com.vibecheck.lifepulse.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

/** Stored amounts use hundredths of the chosen currency. Ledger arithmetic uses Long. */
object Money {
    val supportedCurrencies = listOf("INR", "USD", "EUR", "GBP", "AUD", "CAD")
    var currencyCode: String by mutableStateOf(runCatching {
        Currency.getInstance(Locale.getDefault()).currencyCode
    }.getOrDefault("INR").takeIf { it in supportedCurrencies } ?: "INR")

    fun parse(text: String): Long? = runCatching {
        val value = text.trim().toBigDecimal()
        if (value.signum() < 0) return null
        value.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
    }.getOrNull()

    fun fromLegacy(amount: Double): Long = BigDecimal.valueOf(amount)
        .movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
    fun input(minor: Long): String = BigDecimal.valueOf(minor, 2).toPlainString()
    fun format(minor: Long, code: String = currencyCode): String =
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = Currency.getInstance(code)
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(BigDecimal.valueOf(minor, 2))
}
