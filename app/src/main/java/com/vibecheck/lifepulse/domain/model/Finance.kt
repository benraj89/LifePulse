package com.vibecheck.lifepulse.domain.model

enum class TransactionType {
    EXPENSE, INCOME, TRANSFER, LEND, BORROW, REPAYMENT_RECEIVED, REPAYMENT_PAID;
    val needsCategory get() = this == EXPENSE || this == INCOME
    val isLoan get() = this == LEND || this == BORROW
    val isRepayment get() = this == REPAYMENT_RECEIVED || this == REPAYMENT_PAID
    val moneyIn get() = this == INCOME || this == BORROW || this == REPAYMENT_RECEIVED
}

data class Account(val id: Long, val name: String, val openingMinor: Long)
data class TransactionDraft(
    val id: Long = 0,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountMinor: Long,
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val person: String = "",
    val loanId: Long? = null
)
