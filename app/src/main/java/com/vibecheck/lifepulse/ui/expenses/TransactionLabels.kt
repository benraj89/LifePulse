package com.vibecheck.lifepulse.ui.expenses

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.domain.model.TransactionType

@Composable
internal fun TransactionType.displayLabel(): String = stringResource(when (this) {
    TransactionType.EXPENSE -> R.string.transaction_type_expense
    TransactionType.INCOME -> R.string.transaction_type_income
    TransactionType.TRANSFER -> R.string.transaction_type_transfer
    TransactionType.LEND -> R.string.transaction_type_lend
    TransactionType.BORROW -> R.string.transaction_type_borrow
    TransactionType.REPAYMENT_RECEIVED -> R.string.transaction_type_repayment_received
    TransactionType.REPAYMENT_PAID -> R.string.transaction_type_repayment_paid
})
