package com.vibecheck.lifepulse.core

import com.vibecheck.lifepulse.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class FinanceTest {
    private fun transaction(id: Long, type: TransactionType, amount: Long, account: Long = 1,
        destination: Long? = null, loanId: Long? = null) = Expense(
        id, null, "", "#000000", amount, 0, "", type, account, destination, "Alex", loanId
    )
    @Test fun `money is parsed without rounding valid precision`() {
        assertEquals(10L, Money.parse("0.10"))
        assertEquals(12345L, Money.parse("123.45"))
        assertEquals(100L, Money.parse("1"))
        assertEquals(0L, Money.parse("0"))
        assertNull(Money.parse("1.005"))
        assertNull(Money.parse("-1"))
        assertNull(Money.parse("NaN"))
        assertNull(Money.parse("999999999999999999999999"))
    }
    @Test fun `legacy values are rounded once to hundredths`() {
        assertEquals(1235L, Money.fromLegacy(12.345))
        assertEquals(30L, Money.fromLegacy(0.1 + 0.2))
    }
    @Test fun `transfer changes account balances but preserves total money`() {
        val cash = Account(1, "Cash", 10000)
        val bank = Account(2, "Bank", 5000)
        val transactions = listOf(transaction(1, TransactionType.TRANSFER, 2500, destination = 2))
        assertEquals(7500L, Ledger.balance(cash, transactions))
        assertEquals(7500L, Ledger.balance(bank, transactions))
        assertEquals(15000L, Ledger.balance(cash, transactions) + Ledger.balance(bank, transactions))
    }
    @Test fun `income expenses loans and repayments have correct balance directions`() {
        val transactions = listOf(
            transaction(1, TransactionType.INCOME, 10000),
            transaction(2, TransactionType.EXPENSE, 2000),
            transaction(3, TransactionType.LEND, 3000),
            transaction(4, TransactionType.REPAYMENT_RECEIVED, 1000, loanId = 3),
            transaction(5, TransactionType.BORROW, 5000),
            transaction(6, TransactionType.REPAYMENT_PAID, 2000, loanId = 5)
        )
        assertEquals(9000L, Ledger.balance(Account(1, "Cash", 0), transactions))
        assertEquals(2000L, Ledger.outstanding(transactions[2], transactions))
        assertEquals(3000L, Ledger.outstanding(transactions[4], transactions))
    }
    @Test fun `editing deleting and partially repaying use derived balances`() {
        val loan = transaction(1, TransactionType.LEND, 10000)
        val repayment = transaction(2, TransactionType.REPAYMENT_RECEIVED, 2500, loanId = 1)
        assertEquals(7500L, Ledger.outstanding(loan, listOf(loan, repayment)))
        assertEquals(5000L, Ledger.outstanding(loan, listOf(loan, repayment.copy(amountMinor = 5000))))
        assertEquals(10000L, Ledger.outstanding(loan, listOf(loan)))
        assertEquals(0L, Ledger.outstanding(loan, listOf(loan, repayment.copy(amountMinor = 10000))))
    }
}
