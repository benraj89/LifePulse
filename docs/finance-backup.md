# Finance backup, restore and CSV export

Open **Expenses → folder icon (Backup & export)**. The three separate actions use
Android's Storage Access Framework: `CreateDocument` for `.mbak` and `.csv`, and
`OpenDocument` for restore. No broad storage permission is required. Picking a
restore file opens a replacement confirmation; canceling a picker does nothing.

## Scope

Backups preserve account IDs and opening balances, all categories (including
default and soft-deleted flags), and every transaction field. Transfers retain
both account references. Lend/borrow transactions and repayments retain their
IDs, dates, contact names and loan references.

This database has no standalone contacts table: contacts are distinct nonempty
`expenses.person` values. The backup explicitly lists these names and validates
that they exactly match the restored transactions. It does not invent independent
contact records. Habits, habit logs and the current currency preference are not
part of a finance backup and are retained during restore. Amounts are integer
hundredths of the configured currency; restoring does not convert currencies.

## Version 1 file contract

`BackupWrapper`, `BackupMetadata`, `BackupPayload` and `BackupContact` in
`data/backup/BackupFormat.kt` describe the JSON schema. Arrays are streamed in
production; a whole `BackupPayload` object is never allocated.

```json
{
  "metadata": {
    "version": 1,
    "timestamp": "2026-10-04T18:00:00Z",
    "checksum": "<64 lowercase hexadecimal SHA-256 characters>"
  },
  "data": {
    "accounts": [],
    "categories": [],
    "transactions": [],
    "contacts": []
  }
}
```

The snippet illustrates the structure; a valid backup includes at least one
account and a real checksum. Each record uses the following fixed field order:

| Array | Fields |
| --- | --- |
| accounts | id, name, openingMinor |
| categories | id, name, colorHex, isDefault, isDeleted, kind |
| transactions | id, categoryId, amountMinor, dateTimestamp, note, type, accountId, toAccountId, person, loanId |
| contacts | name |

Null references are written explicitly. IDs and amounts are JSON integers, not
strings or floating point values. All non-repayment transactions are exported
before repayments; IDs are ascending within each group. Contacts are sorted
using SQLite's binary collation. Every referenced row must already exist when
its dependent transaction is read.

The checksum is SHA-256 of **only the compact canonical UTF-8 data object**, without
a BOM or trailing newline, as emitted by `BackupFormat.writePayload` using
Android `JsonWriter`. Import reconstructs this same byte representation while
streaming records and compares decoded 32-byte digests using
`MessageDigest.isEqual`. JSON whitespace differences are accepted. Missing,
unknown, duplicate or reordered fields, unsupported versions, invalid UTF-8,
trailing content and checksum mismatches are rejected.

An unkeyed SHA-256 checksum detects corruption and modifications that leave the
checksum unchanged. It is **not proof of authorship**: someone who changes the
data can calculate a new checksum. Semantic validation and parameterized Room
inserts therefore apply even to files with matching checksums. Backups and CSV
files are readable, unencrypted exports, not password-protected archives.

## Restore safety and performance

`FinanceBackupRepository` switches all file operations to `Dispatchers.IO`.
`BackupDao.readSnapshot` holds a Room `@Transaction` for a consistent paged
snapshot. Keyset pages contain at most 500 records. External provider writes
happen after the snapshot has been written to a private temporary file, so a
slow provider does not hold the live database transaction open.

Restore streams into a separate private, on-disk Room database. It validates
the version, syntax, checksum, record limits, foreign keys, category/type
agreement, different transfer accounts, loan direction, repayment dates and
total repayments. Duplicate IDs/names fail with `ABORT` inserts. No live records
are deleted until the entire file passes. `BackupDao.replaceFinance` then
deletes dependents first and inserts parents first in one `@Transaction`.
An insertion error or cancellation inside that transaction rolls it back.
Temporary files and database sidecars are cleaned up on normal completion,
failure and cancellation; the OS may retain private cache files after process
termination until cache eviction.

Resource guards allow up to 512 MiB per file, 1,000,000 rows per primary array,
16,384 UTF-16 code units per text field (50 for account names), and nesting depth
8. A lexical guard bounds raw token length before JSON parsing allocates large
strings. Dates must fit years 0001–9999 UTC. Export verifies its generated backup
with the same importer before reporting success, so unsupported local data
produces an error rather than an unusable backup. Staging requires additional
free private storage; storage failures leave live finance records intact.

## CSV

CSV is export-only and includes all transaction types and all dates/accounts,
regardless of current screen filters. Columns are:

`DateTime (UTC), Type, Amount, Category, Account, ToAccount, Contact, Note`

Dates use `yyyy-MM-dd HH:mm:ss` in UTC with `Locale.ROOT`; money uses exact decimal
hundredths with a period and no grouping. Names replace relational IDs. Files use
UTF-8 with a BOM for Excel, CRLF record separators and quoted/escaped fields.
Text starting with spreadsheet formula prefixes, including after whitespace,
is prefixed with an apostrophe. This preserves it as text in spreadsheet imports.
CSV does not contain all relational metadata and cannot be restored.

## Validation

`BackupFormatTest` covers known SHA-256 vectors, constant-length checksum
validation, CSV escaping/formula protection and locale-independent dates.
`BackupDatabaseTest` uses isolated Room databases to exercise round trips for
all transaction types, deleted categories, invalid files and relationships,
rollback after a forced live insertion failure, cancellation, off-main-thread
I/O and a 5,507-transaction restore spanning many pages.

References: [Android SAF](https://developer.android.com/training/data-storage/shared/documents-files)
and [Room transactions](https://developer.android.com/reference/androidx/room/Transaction).
