package com.vibecheck.lifepulse.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

/** Integer amounts are minor units of the ledger's saved currency, never floating point. */
object Money {
    val supportedCurrencies: List<String> get() = CurrencyCatalog.entries.map { it.code }
    // Navigation hides financial UI until its saved currency has been loaded and confirmed.
    var currencyCode: String by mutableStateOf("USD")
        internal set

    fun suggestedCurrency(locale: Locale = Locale.getDefault()): String? = runCatching {
        Currency.getInstance(locale).currencyCode
    }.getOrNull()?.takeIf { CurrencyCatalog.find(it) != null }

    fun fractionDigits(code: String = currencyCode): Int = requireNotNull(CurrencyCatalog.find(code)).fractionDigits

    private fun normalize(text: String, locale: Locale, allowNegative: Boolean): String? {
        val decimal = DecimalFormatSymbols.getInstance(locale).decimalSeparator
        val result = StringBuilder()
        for ((index, char) in text.trim().withIndex()) {
            when {
                char == '-' && index == 0 && allowNegative -> result.append('-')
                char == '.' || char == decimal -> result.append('.')
                Character.digit(char, 10) >= 0 -> result.append(Character.digit(char, 10))
                else -> return null // Group separators are not accepted in amount entry.
            }
        }
        return result.toString().takeIf { it.count { char -> char == '.' } <= 1 }
    }

    fun acceptsInput(text: String, allowNegative: Boolean = false, code: String = currencyCode,
                     locale: Locale = Locale.getDefault()): Boolean {
        val normalized = normalize(text, locale, allowNegative) ?: return false
        val unsigned = normalized.removePrefix("-")
        if (unsigned.substringBefore('.').length > 12) return false
        return if ('.' in unsigned) fractionDigits(code) > 0 && unsigned.substringAfter('.').length <= fractionDigits(code)
            else true
    }

    fun parse(text: String, code: String = currencyCode, locale: Locale = Locale.getDefault(),
              allowNegative: Boolean = false): Long? = runCatching {
        if (!acceptsInput(text, allowNegative, code, locale)) return null
        val value = (normalize(text, locale, allowNegative) ?: return null).toBigDecimal()
        value.movePointRight(fractionDigits(code)).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
    }.getOrNull()

    /** Only the old, six-currency database used a fixed scale of two. */
    fun fromLegacy(amount: Double): Long = BigDecimal.valueOf(amount)
        .movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()

    fun decimal(minor: Long, code: String = currencyCode): String =
        BigDecimal.valueOf(minor, fractionDigits(code)).toPlainString()

    fun input(minor: Long, code: String = currencyCode, locale: Locale = Locale.getDefault()): String =
        decimal(minor, code).replace('.', DecimalFormatSymbols.getInstance(locale).decimalSeparator)

    fun format(minor: Long, code: String = currencyCode, locale: Locale = Locale.getDefault()): String {
        val digits = fractionDigits(code)
        val currency = runCatching { Currency.getInstance(code) }.getOrNull()
        val formatter = if (currency != null) NumberFormat.getCurrencyInstance(locale).apply { this.currency = currency }
            else NumberFormat.getNumberInstance(locale)
        formatter.minimumFractionDigits = digits
        formatter.maximumFractionDigits = digits
        val amount = formatter.format(BigDecimal.valueOf(minor, digits))
        // New ISO codes can precede the data shipped in older Android versions.
        return if (currency != null) amount else "$amount\u00a0$code"
    }
}
