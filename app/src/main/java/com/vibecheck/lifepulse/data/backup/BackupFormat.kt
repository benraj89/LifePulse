package com.vibecheck.lifepulse.data.backup

import com.vibecheck.lifepulse.R
import androidx.annotation.StringRes

import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import com.vibecheck.lifepulse.data.local.dao.*
import com.vibecheck.lifepulse.data.local.entity.*
import com.vibecheck.lifepulse.domain.model.TransactionType
import com.vibecheck.lifepulse.core.CurrencyCatalog
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.*
import java.nio.charset.CodingErrorAction
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.time.Instant

/** The wire contract. Production code streams these arrays rather than building this whole object. */
data class BackupWrapper(val metadata: BackupMetadata, val data: BackupPayload)
data class BackupMetadata(val version: Int, val timestamp: String, val checksum: String)
data class BackupContact(val name: String)
data class BackupPayload(
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<ExpenseEntity>,
    val contacts: List<BackupContact>
)

class InvalidBackupException(@StringRes val messageRes: Int, vararg val formatArgs: Any) : IOException()

object BackupChecksum {
    fun sha256(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(32 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        return hex(digest.digest())
    }

    fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }

    /** Constant-time comparison of decoded, fixed-length SHA-256 values. */
    fun verify(expected: String, actual: ByteArray): Boolean {
        if (!expected.matches(Regex("[0-9a-f]{64}"))) return false
        val decoded = ByteArray(32) { expected.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        return MessageDigest.isEqual(decoded, actual)
    }
}

/** Fixed field order and compact UTF-8 JSON provide checksum canonicalization. V2 binds currency to amounts. */
object BackupFormat {
    const val VERSION = 2
    val LEGACY_CURRENCIES = CurrencyCatalog.LEGACY_CODES
    const val MAX_FILE_BYTES = 512L * 1024 * 1024
    private const val MAX_ROWS = 1_000_000
    private const val MAX_TEXT = 16_384
    private const val MAX_AMOUNT = 100_000_000_000_000L

    fun writer(output: OutputStream) = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))

    suspend fun writePayload(dao: BackupDao, writer: JsonWriter) {
        val settings = dao.settings()
        val currency = settings?.currencyCode?.let(CurrencyCatalog::find)
        checkBackup(settings?.confirmed == true && currency != null, R.string.error_currency_required)
        writer.beginObject()
        writeCurrency(writer, currency!!.code, currency.fractionDigits)
        writer.name("accounts").beginArray()
        dao.forEachAccountPage { rows -> rows.forEach { writeAccount(writer, it) } }
        writer.endArray().name("categories").beginArray()
        dao.forEachCategoryPage { rows -> rows.forEach { writeCategory(writer, it) } }
        writer.endArray().name("transactions").beginArray()
        dao.forEachTransactionPage { rows -> rows.forEach { writeTransaction(writer, it) } }
        writer.endArray().name("contacts").beginArray()
        var after = ""
        while (true) {
            currentCoroutineContext().ensureActive()
            val contacts = dao.contacts(after, BACKUP_PAGE_SIZE)
            if (contacts.isEmpty()) break
            contacts.forEach { writeContact(writer, it) }
            after = contacts.last()
        }
        writer.endArray().endObject()
        writer.flush()
    }

    fun writeContainer(metadata: BackupMetadata, payload: File, output: OutputStream) {
        // Only app-generated JSON is copied as raw JSON. No untrusted raw values reach this writer.
        val prefix = StringWriter()
        JsonWriter(prefix).apply {
            beginObject()
            name("version").value(metadata.version.toLong())
            name("timestamp").value(metadata.timestamp)
            name("checksum").value(metadata.checksum)
            endObject()
            close()
        }
        output.write(("{\"metadata\":" + prefix.toString() + ",\"data\":").toByteArray(Charsets.UTF_8))
        payload.inputStream().use { it.copyTo(output) }
        output.write('}'.code)
    }

    /** Only a private staging database is mutated here; the live DB is untouched until this returns. */
    suspend fun readInto(input: InputStream, stage: BackupDao, legacyCurrency: String? = null): BackupMetadata {
        val decoder = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val reader = JsonReader(GuardedJsonReader(InputStreamReader(BoundedInputStream(input, MAX_FILE_BYTES), decoder)))
        reader.isLenient = false
        reader.beginObject()
        reader.field("metadata"); reader.beginObject()
        reader.field("version")
        val version = reader.integer()
        checkBackup(version == 1L || version == VERSION.toLong(), R.string.backup_error_version)
        if (version == 1L) checkBackup(legacyCurrency in LEGACY_CURRENCIES, R.string.backup_error_legacy_currency)
        reader.field("timestamp")
        val timestamp = reader.text()
        checkBackup(runCatching { Instant.parse(timestamp) }.isSuccess, R.string.backup_error_timestamp)
        reader.field("checksum")
        val checksum = reader.text()
        checkBackup(checksum.matches(Regex("[0-9a-f]{64}")), R.string.backup_error_checksum)
        reader.endObject()
        reader.field("data"); reader.beginObject()

        val currencyCode: String
        if (version >= 2L) {
            reader.field("currency"); reader.beginObject()
            reader.field("code"); currencyCode = reader.text()
            reader.field("fractionDigits"); val digits = reader.integer()
            checkBackup(CurrencyCatalog.find(currencyCode)?.fractionDigits?.toLong() == digits, R.string.backup_error_currency)
            reader.endObject()
        } else currencyCode = legacyCurrency!!

        val digest = MessageDigest.getInstance("SHA-256")
        val sink = object : OutputStream() {
            override fun write(b: Int) = Unit
            override fun write(b: ByteArray, off: Int, len: Int) = Unit
        }
        val canonical = writer(DigestOutputStream(sink, digest))
        canonical.beginObject()
        if (version >= 2L) writeCurrency(canonical, currencyCode, CurrencyCatalog.find(currencyCode)!!.fractionDigits)
        canonical.name("accounts").beginArray()
        reader.field("accounts")
        var accountCount = 0
        readArray(reader, { readAccount(reader) }) { rows ->
            accountCount += rows.size
            stage.insertAccounts(rows)
            rows.forEach { writeAccount(canonical, it) }
        }
        checkBackup(accountCount > 0, R.string.backup_error_account_required)
        canonical.endArray().name("categories").beginArray()
        reader.field("categories")
        readArray(reader, { readCategory(reader) }) { rows ->
            stage.insertCategories(rows)
            rows.forEach { writeCategory(canonical, it) }
        }
        canonical.endArray().name("transactions").beginArray()
        reader.field("transactions")
        readArray(reader, { readTransaction(reader) }) { rows ->
            stage.insertTransactions(rows)
            rows.forEach { writeTransaction(canonical, it) }
        }
        canonical.endArray().name("contacts").beginArray()
        reader.field("contacts"); reader.beginArray()
        // Contacts are derived from person fields in this app's schema. Require exact agreement.
        var after = ""
        while (true) {
            currentCoroutineContext().ensureActive()
            val names = stage.contacts(after, BACKUP_PAGE_SIZE)
            if (names.isEmpty()) break
            for (name in names) {
                checkBackup(reader.hasNext(), R.string.backup_error_contact_missing)
                reader.beginObject(); reader.field("name")
                checkBackup(reader.text() == name, R.string.backup_error_contacts_mismatch)
                reader.endObject()
                writeContact(canonical, name)
            }
            after = names.last()
        }
        reader.endArray(); reader.endObject(); reader.endObject()
        checkBackup(reader.peek() == JsonToken.END_DOCUMENT, R.string.backup_error_trailing_content)
        canonical.endArray().endObject(); canonical.close()
        checkBackup(BackupChecksum.verify(checksum, digest.digest()), R.string.backup_error_checksum_failed)
        checkBackup(stage.invalidRelations() == 0L && stage.overpaidLoans() == 0L, R.string.backup_error_relationships)
        stage.saveSettings(FinanceSettingsEntity(currencyCode = currencyCode, confirmed = true))
        return BackupMetadata(version.toInt(), timestamp, checksum)
    }

    private suspend fun <T> readArray(reader: JsonReader, read: () -> T, insert: suspend (List<T>) -> Unit) {
        reader.beginArray()
        var count = 0
        val batch = ArrayList<T>(BACKUP_PAGE_SIZE)
        while (reader.hasNext()) {
            currentCoroutineContext().ensureActive()
            checkBackup(++count <= MAX_ROWS, R.string.backup_error_record_limit)
            batch += read()
            if (batch.size == BACKUP_PAGE_SIZE) { insert(batch); batch.clear() }
        }
        if (batch.isNotEmpty()) insert(batch)
        reader.endArray()
    }

    private fun writeCurrency(w: JsonWriter, code: String, digits: Int) {
        w.name("currency").beginObject().name("code").value(code)
            .name("fractionDigits").value(digits.toLong()).endObject()
    }

    private fun writeAccount(w: JsonWriter, a: AccountEntity) {
        w.beginObject().name("id").value(a.id).name("name").value(a.name)
            .name("openingMinor").value(a.openingMinor).endObject()
    }
    private fun writeCategory(w: JsonWriter, c: CategoryEntity) {
        w.beginObject().name("id").value(c.id).name("name").value(c.name)
            .name("colorHex").value(c.colorHex).name("isDefault").value(c.isDefault)
            .name("isDeleted").value(c.isDeleted).name("kind").value(c.kind).endObject()
    }
    private fun writeTransaction(w: JsonWriter, t: ExpenseEntity) {
        w.beginObject().name("id").value(t.id).name("categoryId").value(t.categoryId)
            .name("amountMinor").value(t.amountMinor).name("dateTimestamp").value(t.dateTimestamp)
            .name("note").value(t.note).name("type").value(t.type).name("accountId").value(t.accountId)
            .name("toAccountId").value(t.toAccountId).name("person").value(t.person)
            .name("loanId").value(t.loanId).endObject()
    }
    private fun writeContact(w: JsonWriter, name: String) { w.beginObject().name("name").value(name).endObject() }

    private fun readAccount(r: JsonReader): AccountEntity {
        r.beginObject()
        r.field("id"); val id = r.id()
        r.field("name"); val name = r.text().also { checkBackup(it.isNotBlank() && it.length <= 50, R.string.backup_error_account_name) }
        r.field("openingMinor"); val opening = r.integer()
        r.endObject()
        return AccountEntity(id, name, opening)
    }
    private fun readCategory(r: JsonReader): CategoryEntity {
        r.beginObject()
        r.field("id"); val id = r.id()
        r.field("name"); val name = r.text().also { checkBackup(it.isNotBlank(), R.string.backup_error_category_name) }
        r.field("colorHex"); val color = r.text().also { checkBackup(it.matches(Regex("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")), R.string.backup_error_category_color) }
        r.field("isDefault"); val default = r.nextBoolean()
        r.field("isDeleted"); val deleted = r.nextBoolean()
        r.field("kind"); val kind = r.text().also { checkBackup(it in listOf("EXPENSE", "INCOME"), R.string.backup_error_category_type) }
        r.endObject()
        return CategoryEntity(id, name, color, default, deleted, kind)
    }
    private fun readTransaction(r: JsonReader): ExpenseEntity {
        r.beginObject()
        r.field("id"); val id = r.id()
        r.field("categoryId"); val category = r.nullableId()
        r.field("amountMinor"); val amount = r.integer()
        checkBackup(amount in 1..MAX_AMOUNT, R.string.backup_error_amount)
        r.field("dateTimestamp"); val date = r.integer()
        // Keep exported dates within four-digit calendar years.
        checkBackup(date in -62_135_596_800_000L..253_402_300_799_999L, R.string.backup_error_date)
        r.field("note"); val note = r.text()
        r.field("type"); val typeName = r.text()
        val type = TransactionType.entries.firstOrNull { it.name == typeName }
            ?: throw InvalidBackupException(R.string.backup_error_transaction_type)
        r.field("accountId"); val account = r.id()
        r.field("toAccountId"); val toAccount = r.nullableId()
        r.field("person"); val person = r.text()
        r.field("loanId"); val loan = r.nullableId()
        r.endObject()
        checkBackup((category != null) == type.needsCategory, R.string.backup_error_category_reference)
        checkBackup((toAccount != null) == (type == TransactionType.TRANSFER) && toAccount != account, R.string.backup_error_transfer_accounts)
        checkBackup((loan != null) == type.isRepayment && loan != id, R.string.backup_error_loan_reference)
        checkBackup(!type.isLoan || person.isNotBlank(), R.string.backup_error_loan_contact)
        checkBackup(type.isLoan || type.isRepayment || person.isEmpty(), R.string.backup_error_unexpected_contact)
        return ExpenseEntity(id, category, amount, date, note, typeName, account, toAccount, person, loan)
    }

    private fun JsonReader.field(expected: String) {
        checkBackup(nextName() == expected, R.string.backup_error_structure, expected)
    }
    private fun JsonReader.integer(): Long {
        checkBackup(peek() == JsonToken.NUMBER, R.string.backup_error_expected_integer)
        val raw = nextString()
        checkBackup(raw.matches(Regex("-?(0|[1-9][0-9]{0,18})")), R.string.backup_error_integer)
        return raw.toLongOrNull() ?: throw InvalidBackupException(R.string.backup_error_integer_range)
    }
    private fun JsonReader.id() = integer().also { checkBackup(it in 1 until Long.MAX_VALUE, R.string.backup_error_record_id) }
    private fun JsonReader.nullableId(): Long? = if (peek() == JsonToken.NULL) { nextNull(); null } else id()
    private fun JsonReader.text(): String {
        checkBackup(peek() == JsonToken.STRING, R.string.backup_error_expected_text)
        return nextString().also { checkBackup(it.length <= MAX_TEXT && '\u0000' !in it, R.string.backup_error_text) }
    }
    private fun checkBackup(condition: Boolean, @StringRes messageRes: Int, vararg formatArgs: Any) {
        if (!condition) throw InvalidBackupException(messageRes, *formatArgs)
    }
}

/** Bounds bytes even for providers that do not report a file size. */
internal class BoundedInputStream(input: InputStream, private val limit: Long) : FilterInputStream(input) {
    private var count = 0L
    private fun add(n: Int) { if (n > 0 && (count + n).also { count = it } > limit) throw InvalidBackupException(R.string.backup_error_size) }
    override fun read(): Int = `in`.read().also { if (it >= 0) add(1) }
    override fun read(b: ByteArray, off: Int, len: Int): Int = `in`.read(b, off, len).also(::add)
}

/** Reject oversized tokens/deep nesting BEFORE JsonReader allocates an unbounded string. */
private class GuardedJsonReader(reader: Reader) : FilterReader(reader) {
    private var quoted = false
    private var escaped = false
    private var tokenLength = 0
    private var depth = 0
    override fun read(buffer: CharArray, offset: Int, count: Int): Int {
        val n = super.read(buffer, offset, count)
        for (i in offset until offset + n) inspect(buffer[i])
        return n
    }
    override fun read(): Int = super.read().also { if (it >= 0) inspect(it.toChar()) }
    private fun inspect(c: Char) {
        if (quoted) {
            tokenLength++
            if (escaped) escaped = false else if (c == '\\') escaped = true else if (c == '"') { quoted = false; tokenLength = 0 }
        } else when (c) {
            '"' -> { quoted = true; tokenLength = 0 }
            '{', '[' -> { depth++; tokenLength = 0 }
            '}', ']' -> { depth--; tokenLength = 0 }
            ',', ':', ' ', '\n', '\r', '\t' -> tokenLength = 0
            else -> tokenLength++
        }
        if (depth > 8 || tokenLength > 100_000) throw InvalidBackupException(R.string.backup_error_structure_size)
    }
}
