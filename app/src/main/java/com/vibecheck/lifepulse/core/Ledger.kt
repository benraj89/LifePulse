package com.vibecheck.lifepulse.core

import com.vibecheck.lifepulse.domain.model.*

/** Transfers and loans affect balances, but do not inflate spending or income. */
object Ledger {
    fun balance(account: Account, transactions: List<Expense>): Long =
        transactions.fold(account.openingMinor) { balance, transaction ->
            val outgoing = if (transaction.accountId == account.id) {
                if (transaction.type.moneyIn) transaction.amountMinor else -transaction.amountMinor
            } else 0L
            val incoming = if (transaction.type == TransactionType.TRANSFER &&
                transaction.toAccountId == account.id) transaction.amountMinor else 0L
            Math.addExact(balance, Math.addExact(outgoing, incoming))
        }
    fun outstanding(loan: Expense, transactions: List<Expense>): Long =
        loan.amountMinor - transactions.filter { it.loanId == loan.id }.sumOf { it.amountMinor }
}
