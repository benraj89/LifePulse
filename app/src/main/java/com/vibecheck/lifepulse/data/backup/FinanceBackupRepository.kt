package com.vibecheck.lifepulse.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vibecheck.lifepulse.data.local.LifePulseDatabase
import com.vibecheck.lifepulse.data.local.dao.BACKUP_PAGE_SIZE
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.*
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: LifePulseDatabase
) {
    private val operationLock = Mutex()

    suspend fun saveBackup(uri: Uri) = withContext(Dispatchers.IO) {
        operationLock.withLock {
            withBackupFile { file ->
                val output = context.contentResolver.openOutputStream(uri, "wt")
                    ?: throw IOException("Could not open the destination.")
                output.buffered().use { destination -> copyCancellable(file, destination) }
            }
        }
    }

    suspend fun restoreBackup(uri: Uri) = withContext(Dispatchers.IO) {
        operationLock.withLock {
            val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Could not open the backup.")
            input.buffered().use { restore(it) }
        }
    }

    suspend fun saveCsv(uri: Uri) = withContext(Dispatchers.IO) {
        operationLock.withLock {
            withTempFile(".csv") { file ->
                file.outputStream().buffered().use { writeCsv(it) }
                val output = context.contentResolver.openOutputStream(uri, "wt")
                    ?: throw IOException("Could not open the destination.")
                output.buffered().use { copyCancellable(file, it) }
            }
        }
    }

    /** Stream entry points also make file/Room integration testable without a document provider. */
    internal suspend fun writeBackup(output: OutputStream) = withContext(Dispatchers.IO) {
        withBackupFile { copyCancellable(it, output) }
    }

    private suspend fun withBackupFile(block: suspend (File) -> Unit) {
        withTempFile(".payload") { payload ->
            val timestamp = Instant.now().toString()
            payload.outputStream().buffered().use { output ->
                BackupFormat.writer(output).use { writer ->
                    database.backupDao().readSnapshot { BackupFormat.writePayload(it, writer) }
                }
            }
            currentCoroutineContext().ensureActive()
            if (payload.length() > BackupFormat.MAX_FILE_BYTES - 1024) throw IOException("Backup exceeds the 512 MB limit.")
            val checksum = payload.inputStream().buffered().use(BackupChecksum::sha256)
            withTempFile(".mbak") { file ->
                file.outputStream().buffered().use { BackupFormat.writeContainer(BackupMetadata(BackupFormat.VERSION, timestamp, checksum), payload, it) }
                // Check our output against the same rules as restore, so we never report success
                // for a safety-net file that this version would refuse to restore.
                withStagingDatabase { stage ->
                    file.inputStream().buffered().use { input ->
                        stage.backupDao().readSnapshot { BackupFormat.readInto(input, it) }
                    }
                }
                block(file)
            }
        }
    }

    internal suspend fun restore(input: InputStream) = withContext(Dispatchers.IO) {
        withStagingDatabase { stage ->
            try {
                stage.backupDao().readSnapshot { BackupFormat.readInto(input, it) }
            } catch (e: CancellationException) { throw e
            } catch (e: InvalidBackupException) { throw e
            } catch (e: Exception) {
                throw InvalidBackupException("The backup is unreadable or contains invalid data.")
            }
            currentCoroutineContext().ensureActive()
            database.backupDao().replaceFinance(stage.backupDao())
        }
    }

    internal suspend fun writeCsv(output: OutputStream) = withContext(Dispatchers.IO) {
        val writer = output.writer(Charsets.UTF_8).buffered()
        // BOM helps Excel recognize non-ASCII contact/category names. RFC 4180 line endings.
        writer.write("\uFEFFDateTime (UTC),Type,Amount,Category,Account,ToAccount,Contact,Note\r\n")
        database.backupDao().readSnapshot { dao ->
            var after = 0L
            while (true) {
                currentCoroutineContext().ensureActive()
                val rows = dao.csvRows(after, BACKUP_PAGE_SIZE)
                if (rows.isEmpty()) break
                for (row in rows) {
                    val fields = listOf(
                        CsvFormat.date(row.dateTimestamp), row.type,
                        BigDecimal.valueOf(row.amountMinor, 2).toPlainString(),
                        row.category, row.account, row.toAccount, row.contact, row.note
                    )
                    writer.write(fields.mapIndexed { index, value -> CsvFormat.cell(value, protectFormula = index >= 3) }.joinToString(","))
                    writer.write("\r\n")
                }
                after = rows.last().id
            }
        }
        writer.flush()
    }

    private suspend fun withTempFile(suffix: String, block: suspend (File) -> Unit) {
        val file = File.createTempFile("finance-", suffix, context.cacheDir)
        try { block(file) } finally { file.delete() }
    }

    private suspend fun withStagingDatabase(block: suspend (LifePulseDatabase) -> Unit) {
        val file = File.createTempFile("finance-stage-", ".db", context.cacheDir)
        val stage = Room.databaseBuilder(context, LifePulseDatabase::class.java, file.absolutePath)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE).build()
        try { block(stage) } finally {
            stage.close()
            // deleteDatabase also removes SQLite sidecars; path is generated privately above.
            android.database.sqlite.SQLiteDatabase.deleteDatabase(file)
        }
    }

    private suspend fun copyCancellable(file: File, output: OutputStream) {
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                currentCoroutineContext().ensureActive()
                val size = input.read(buffer)
                if (size < 0) break
                output.write(buffer, 0, size)
            }
        }
    }
}
