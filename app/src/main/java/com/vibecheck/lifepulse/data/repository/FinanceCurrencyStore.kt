package com.vibecheck.lifepulse.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.core.CurrencyCatalog
import com.vibecheck.lifepulse.core.Money
import com.vibecheck.lifepulse.data.local.LifePulseDatabase
import com.vibecheck.lifepulse.data.local.entity.FinanceSettingsEntity
import java.util.Locale

/** Currency lives with its amounts. Preferences are read only for migration. */
class FinanceCurrencyStore(private val context: Context, private val database: LifePulseDatabase,
                           private val locale: Locale = Locale.getDefault()) {
    suspend fun initialize(): FinanceSettingsEntity = database.withTransaction {
        val dao = database.financeSettingsDao()
        dao.get() ?: run {
            val legacy = context.getSharedPreferences("finance_settings", Context.MODE_PRIVATE).getString("currency", null)
                ?.takeIf { it in CurrencyCatalog.LEGACY_CODES }
            val hasData = dao.hasFinancialData()
            // Missing legacy preferences cannot reveal a historical denomination. Ask explicitly.
            FinanceSettingsEntity(currencyCode = legacy ?: if (hasData) null else Money.suggestedCurrency(locale),
                confirmed = legacy != null).also { dao.save(it) }
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
