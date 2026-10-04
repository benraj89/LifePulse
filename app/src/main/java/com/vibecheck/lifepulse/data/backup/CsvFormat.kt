package com.vibecheck.lifepulse.data.backup

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object CsvFormat {
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT).withZone(ZoneOffset.UTC)
    fun date(timestamp: Long): String = dateFormat.format(Instant.ofEpochMilli(timestamp))

    fun cell(value: String, protectFormula: Boolean = true): String {
        // Quoting alone does not prevent Excel from evaluating formulas. Include control/
        // whitespace prefixes because spreadsheet importers may trim them before evaluation.
        val significant = value.dropWhile { it.isWhitespace() || it.isISOControl() || it == '\uFEFF' }
        val dangerous = significant.firstOrNull() in listOf('=', '+', '-', '@') ||
            value.firstOrNull() in listOf('\t', '\r', '\n')
        val safe = if (protectFormula && dangerous) "'$value" else value
        return "\"${safe.replace("\"", "\"\"")}\""
    }
}
