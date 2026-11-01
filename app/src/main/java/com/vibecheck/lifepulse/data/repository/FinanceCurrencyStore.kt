package com.vibecheck.lifepulse.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.core.CurrencyCatalog
import com.vibecheck.lifepulse.core.Money
import com.vibecheck.lifepulse.data.local.LifePulseDatabase
import com.vibecheck.lifepulse.data.local.entity.FinanceSettingsEntity
import java.util.Locale

/** Currency lives with its amounts in the release database. */
class FinanceCurrencyStore(private val context: Context, private val database: LifePulseDatabase,
                           private val locale: Locale = Locale.getDefault()) {
    suspend fun initialize(): FinanceSettingsEntity = database.withTransaction {
        val dao = database.financeSettingsDao()
        dao.get() ?: run {
            val hasData = dao.hasFinancialData()
            // Existing amounts without a setting require explicit recovery, never a region guess.
            FinanceSettingsEntity(currencyCode = if (hasData) null else Money.suggestedCurrency(locale),
                confirmed = false).also { dao.save(it) }
        }
    }.also { it.currencyCode?.let { code -> Money.currencyCode = code } }

    suspend fun select(code: String) {
        require(CurrencyCatalog.find(code) != null) { context.getString(R.string.error_currency_invalid) }
        initialize()
        database.withTransaction {
            val dao = database.financeSettingsDao()
            val current = dao.get()
            val recoveringLegacyCurrency = current?.confirmed == false && current.currencyCode == null && code in CurrencyCatalog.LEGACY_CODES
            require(current?.currencyCode == code || !dao.hasFinancialData() || recoveringLegacyCurrency) {
                context.getString(R.string.currency_locked_hint)
            }
            dao.save(FinanceSettingsEntity(currencyCode = code, confirmed = true))
        }
        Money.currencyCode = code
    }
}
