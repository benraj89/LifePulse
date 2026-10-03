package com.vibecheck.lifepulse.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["toAccountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = ExpenseEntity::class, parentColumns = ["id"], childColumns = ["loanId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["dateTimestamp"]), Index("accountId"), Index("toAccountId"), Index("loanId")
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val categoryId: Long? = null,
    val amountMinor: Long,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val type: String = "EXPENSE",
    val accountId: Long = 1,
    val toAccountId: Long? = null,
    val person: String = "",
    val loanId: Long? = null
)

