package com.vibecheck.lifepulse

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vibecheck.lifepulse.data.backup.*
import com.vibecheck.lifepulse.data.local.LifePulseDatabase
import com.vibecheck.lifepulse.data.local.dao.*
import com.vibecheck.lifepulse.data.local.entity.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class BackupDatabaseTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var source: LifePulseDatabase
    private lateinit var target: LifePulseDatabase
    private lateinit var exporter: FinanceBackupRepository
    private lateinit var importer: FinanceBackupRepository

    @Before fun setup(): Unit = runBlocking {
        source = Room.inMemoryDatabaseBuilder(context, LifePulseDatabase::class.java).build()
        target = Room.inMemoryDatabaseBuilder(context, LifePulseDatabase::class.java).build()
        source.financeSettingsDao().save(FinanceSettingsEntity(currencyCode = "USD", confirmed = true))
        target.financeSettingsDao().save(FinanceSettingsEntity(currencyCode = "EUR", confirmed = true))
        exporter = FinanceBackupRepository(context, source)
        importer = FinanceBackupRepository(context, target)
        source.backupDao().insertAccounts(listOf(AccountEntity(1, "Cash", 1234), AccountEntity(2, "Bank", -123)))
        source.backupDao().insertCategories(listOf(
            CategoryEntity(1, "Food", "#FF7043", isDeleted = true),
            CategoryEntity(2, "Salary", "#66BB6A", isDefault = true, kind = "INCOME")))
        source.backupDao().insertTransactions(listOf(
            ExpenseEntity(10, amountMinor = 10000, dateTimestamp = 0, type = "LEND", person = "Zoë 王"),
            ExpenseEntity(11, amountMinor = 20000, dateTimestamp = 0, type = "BORROW", person = "Alex"),
            ExpenseEntity(12, amountMinor = 100, dateTimestamp = 0, type = "TRANSFER", toAccountId = 2),
            ExpenseEntity(1, categoryId = 1, amountMinor = 1234, dateTimestamp = 0, note = "Lunch, \"tea\"\nnext line"),
            ExpenseEntity(2, categoryId = 2, amountMinor = 90000, dateTimestamp = 0, type = "INCOME"),
            ExpenseEntity(3, amountMinor = 5000, dateTimestamp = 1000, type = "REPAYMENT_RECEIVED", accountId = 2, person = "Zoë 王", loanId = 10),
            ExpenseEntity(4, amountMinor = 6000, dateTimestamp = 1000, type = "REPAYMENT_PAID", person = "Alex", loanId = 11)))
        target.backupDao().insertAccounts(listOf(AccountEntity(99, "Keep me")))
        target.backupDao().insertTransactions(listOf(ExpenseEntity(99, amountMinor = 100, type = "LEND", accountId = 99, person = "Original")))
        target.habitDao().insertHabit(HabitEntity(1, "Walk", "DAILY"))
    }
    @After fun cleanup() { source.close(); target.close() }

    private suspend fun backup(): String = ByteArrayOutputStream().also { exporter.writeBackup(it) }.toString("UTF-8")
    private suspend fun legacyBackup(): String = rehash(backup().replace("\"version\":2", "\"version\":1")
        .replace("\"currency\":{\"code\":\"USD\",\"fractionDigits\":2},", ""))
    private suspend fun transactions(db: LifePulseDatabase): List<ExpenseEntity> {
        val result = mutableListOf<ExpenseEntity>()
        db.backupDao().forEachTransactionPage { result.addAll(it) }
        return result
    }
    private suspend fun assertOriginal() {
        assertEquals(listOf(AccountEntity(99, "Keep me")), target.backupDao().accounts(0, 500))
        assertEquals(99L, transactions(target).single().id)
        assertEquals("Walk", target.habitDao().getHabitById(1)?.title)
        assertEquals("EUR", target.financeSettingsDao().get()?.currencyCode)
    }
    private suspend fun rejected(text: String, legacyCurrency: String? = null) {
        try { importer.restore(text.byteInputStream(), legacyCurrency); fail("Expected restore rejection") }
        catch (_: InvalidBackupException) { }
        assertOriginal()
    }
    private fun rehash(text: String): String {
        val data = text.substringAfter("\"data\":").dropLast(1)
        val hash = BackupChecksum.sha256(data.byteInputStream())
        return text.replace(Regex("\"checksum\":\"[0-9a-f]{64}\""), "\"checksum\":\"$hash\"")
    }

    @Test fun roundTripPreservesEveryFinanceFieldAndHabit() = runBlocking {
        importer.restore(backup().byteInputStream())
        assertEquals(source.backupDao().accounts(0, 500), target.backupDao().accounts(0, 500))
        assertEquals(source.backupDao().categories(0, 500), target.backupDao().categories(0, 500))
        assertEquals(transactions(source), transactions(target))
        assertEquals(source.financeSettingsDao().get(), target.financeSettingsDao().get())
        assertEquals(listOf("Alex", "Zoë 王"), target.backupDao().contacts("", 500))
        assertEquals("Walk", target.habitDao().getHabitById(1)?.title)
    }
    @Test fun checksumMismatchCannotReplaceData() = runBlocking {
        rejected(backup().replace("\"openingMinor\":1234", "\"openingMinor\":1235"))
    }
    @Test fun currencyAndPrecisionAreValidatedAndCoveredByChecksum() = runBlocking {
        val original = backup()
        rejected(original.replace("\"code\":\"USD\"", "\"code\":\"EUR\""))
        rejected(rehash(original.replace("\"fractionDigits\":2", "\"fractionDigits\":3")))
        rejected(rehash(original.replace("\"code\":\"USD\"", "\"code\":\"XXX\"")))
    }
    @Test fun nonTwoDecimalCurrencySurvivesBackupAndIsExplicitInCsv() = runBlocking {
        source.financeSettingsDao().save(FinanceSettingsEntity(currencyCode = "KWD", confirmed = true))
        importer.restore(backup().byteInputStream())
        assertEquals("KWD", target.financeSettingsDao().get()?.currencyCode)
        assertEquals(1234L, target.expenseDao().get(1)?.amountMinor)
        val csv = ByteArrayOutputStream().also { exporter.writeCsv(it) }.toString("UTF-8")
        assertTrue(csv.contains("\"EXPENSE\",\"1.234\",\"KWD\""))
    }
    @Test fun legacyBackupRequiresExplicitOriginalCurrencyBeforeReplacingAnything() = runBlocking {
        val legacy = legacyBackup()
        rejected(legacy)
        rejected(legacy, "JPY") // Fractional yen cannot be restored without rounding.
        importer.restore(legacy.byteInputStream(), "USD")
        assertEquals("USD", target.financeSettingsDao().get()?.currencyCode)
        assertEquals(transactions(source), transactions(target))
    }
    @Test fun legacyBackupAcceptsCurrenciesOutsideTheOriginalSix() = runBlocking {
        importer.restore(legacyBackup().byteInputStream(), "AED")
        assertEquals("AED", target.financeSettingsDao().get()?.currencyCode)
        assertEquals(source.backupDao().accounts(0, 500), target.backupDao().accounts(0, 500))
        assertEquals(transactions(source), transactions(target))
    }
    @Test fun legacyBackupRescalesBalancesLoansAndRepaymentsWithoutChangingValue() = runBlocking {
        importer.restore(legacyBackup().byteInputStream(), "KWD")
        assertEquals("KWD", target.financeSettingsDao().get()?.currencyCode)
        assertEquals(source.backupDao().accounts(0, 500).map { it.copy(openingMinor = it.openingMinor * 10) },
            target.backupDao().accounts(0, 500))
        assertEquals(transactions(source).map { it.copy(amountMinor = it.amountMinor * 10) }, transactions(target))
        assertEquals("Walk", target.habitDao().getHabitById(1)?.title)
    }
    @Test fun legacyBackupSupportsWholeYenWithoutRounding() = runBlocking {
        val legacy = rehash(legacyBackup().replace("\"openingMinor\":1234", "\"openingMinor\":1200")
            .replace("\"openingMinor\":-123", "\"openingMinor\":-100")
            .replace("\"amountMinor\":1234", "\"amountMinor\":1200"))
        importer.restore(legacy.byteInputStream(), "JPY")
        assertEquals("JPY", target.financeSettingsDao().get()?.currencyCode)
        assertEquals(listOf(AccountEntity(1, "Cash", 12), AccountEntity(2, "Bank", -1)), target.backupDao().accounts(0, 500))
        assertEquals(transactions(source).map { it.copy(amountMinor = if (it.id == 1L) 12 else it.amountMinor / 100) },
            transactions(target))
    }
    @Test fun legacyCurrencyFailuresAndCorruptionCannotReplaceExistingData() = runBlocking {
        val legacy = legacyBackup()
        rejected(legacy, "XXX")
        rejected(legacy.replace("\"openingMinor\":1234", "\"openingMinor\":1235"), "KWD")
        rejected(rehash(legacy.replace("\"openingMinor\":1234", "\"openingMinor\":9223372036854775807")), "KWD")
        rejected(rehash(legacy.replace("\"amountMinor\":1234", "\"amountMinor\":100000000000000")), "KWD")
    }
    @Test fun unsupportedVersionCannotReplaceData() = runBlocking { rejected(backup().replace("\"version\":2", "\"version\":999")) }
    @Test fun malformedTruncatedAndTrailingContentAreRejected() = runBlocking {
        val original = backup()
        rejected(original.dropLast(2))
        rejected(original + "{}")
        rejected("Type,Amount,Note\nEXPENSE,12,hello")
        rejected(original.replace("\"version\":2", "\"version\":2,\"version\":2"))
        rejected(original.replace("\"version\":2", "\"version\":\"2\""))
    }
    @Test fun validChecksumDoesNotBypassRelationValidation() = runBlocking {
        val original = backup()
        rejected(rehash(original.replace("\"accountId\":1", "\"accountId\":404")))
        rejected(rehash(original.replace("\"loanId\":10", "\"loanId\":11")))
        rejected(rehash(original.replace("\"toAccountId\":2", "\"toAccountId\":1")))
        rejected(rehash(original.replace("\"amountMinor\":5000", "\"amountMinor\":10001")))
        rejected(rehash(original.replace("\"type\":\"EXPENSE\"", "\"type\":\"INCOME\"")))
    }
    @Test fun duplicateIdsInvalidNumbersAndTypesAreRejected() = runBlocking {
        val original = backup()
        rejected(rehash(original.replace("\"id\":2,\"name\":\"Bank\"", "\"id\":1,\"name\":\"Bank\"")))
        rejected(rehash(original.replace("\"amountMinor\":1234", "\"amountMinor\":1.5")))
        rejected(rehash(original.replace("\"amountMinor\":1234", "\"amountMinor\":-1")))
        rejected(rehash(original.replace("\"amountMinor\":1234", "\"amountMinor\":9223372036854775808")))
        rejected(rehash(original.replace("\"type\":\"EXPENSE\"", "\"type\":\"DROP TABLE expenses\"")))
    }
    @Test fun oversizedStringsAndDeepNestingAreRejected() = runBlocking {
        val original = backup()
        rejected(original.replace("Keep me", "unused").replace("\"Cash\"", "\"" + "x".repeat(100_001) + "\""))
        rejected("[".repeat(1000))
    }
    @Test fun missingContactAndUnknownFieldAreRejected() = runBlocking {
        val original = backup()
        rejected(rehash(original.replace("\"contacts\":[{\"name\":\"Alex\"},", "\"contacts\":[")))
        rejected(rehash(original.replace("\"openingMinor\":1234", "\"extra\":true,\"openingMinor\":1234")))
    }
    @Test fun sqlLookingNotesRemainLiteralData() = runBlocking {
        val note = "'); DROP TABLE expenses; --"
        source.expenseDao().insertExpense(ExpenseEntity(id = 100, amountMinor = 1, type = "LEND", person = "Alex", note = note))
        importer.restore(backup().byteInputStream())
        assertEquals(note, target.expenseDao().get(100)?.note)
        assertEquals(8, transactions(target).size)
    }
    @Test fun failedLiveInsertRollsBackAllDeletions() = runBlocking {
        val original = backup()
        target.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_restore BEFORE INSERT ON expenses BEGIN SELECT RAISE(ABORT, 'rollback test'); END")
        try { importer.restore(original.byteInputStream()); fail("Expected failed insert") }
        catch (_: android.database.sqlite.SQLiteConstraintException) { }
        assertOriginal()
    }
    @Test fun cancelledReadLeavesLiveDatabaseUntouched() = runBlocking {
        val bytes = backup().toByteArray()
        val input = object : InputStream() {
            var index = 0
            override fun read(): Int {
                if (index >= bytes.size / 2) throw CancellationException("Cancel restore")
                return bytes[index++].toInt() and 255
            }
        }
        try { importer.restore(input); fail("Expected cancellation") } catch (_: CancellationException) { }
        assertOriginal()
    }
    @Test fun fileIoRunsAwayFromMainThread() = runBlocking {
        val offMain = AtomicBoolean(true)
        val output = object : ByteArrayOutputStream() {
            override fun write(b: ByteArray, off: Int, len: Int) {
                if (Looper.myLooper() == Looper.getMainLooper()) offMain.set(false)
                super.write(b, off, len)
            }
        }
        withContext(Dispatchers.Main) { exporter.writeBackup(output) }
        val input = object : java.io.FilterInputStream(output.toByteArray().inputStream()) {
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (Looper.myLooper() == Looper.getMainLooper()) offMain.set(false)
                return super.read(b, off, len)
            }
        }
        withContext(Dispatchers.Main) { importer.restore(input); exporter.writeCsv(output) }
        assertTrue(offMain.get())
    }
    @Test fun csvIncludesAllTypesAndEscapesFields() = runBlocking {
        source.expenseDao().insertExpense(ExpenseEntity(amountMinor = 1, dateTimestamp = 0, type = "LEND", person = "=1+1", note = "@SUM(A1)"))
        val csv = ByteArrayOutputStream().also { exporter.writeCsv(it) }.toString("UTF-8")
        assertTrue(csv.startsWith("\uFEFFDateTime (UTC),Type,Amount,Currency,Category,Account,ToAccount,Contact,Note\r\n"))
        assertTrue(csv.contains("\"1970-01-01 00:00:00\",\"EXPENSE\",\"12.34\",\"USD\",\"Food\",\"Cash\""))
        assertTrue(csv.contains("\"Lunch, \"\"tea\"\"\nnext line\""))
        assertTrue(csv.contains("\"'=1+1\",\"'@SUM(A1)\""))
        assertTrue(csv.contains("\"TRANSFER\",\"1.00\",\"USD\",\"\",\"Cash\",\"Bank\""))
        listOf("EXPENSE", "INCOME", "TRANSFER", "LEND", "BORROW", "REPAYMENT_RECEIVED", "REPAYMENT_PAID").forEach { assertTrue(csv.contains("\"$it\"")) }
    }
    @Test fun severalPagesRestoreWithoutMissingOrDuplicatingTransactions() = runBlocking {
        source.backupDao().readSnapshot { dao ->
            for (batch in 0 until 11) dao.insertTransactions((0 until 500).map {
                ExpenseEntity(id = 1000L + batch * 500 + it, amountMinor = 1, dateTimestamp = 0, type = "TRANSFER", toAccountId = 2)
            })
        }
        importer.restore(backup().byteInputStream())
        assertEquals(transactions(source), transactions(target))
        assertEquals(5507, transactions(target).size)
    }
}
