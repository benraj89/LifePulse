package com.vibecheck.lifepulse.ui.expenses

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.data.backup.FinanceBackupRepository
import com.vibecheck.lifepulse.data.backup.InvalidBackupException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BackupOperation { BACKUP, RESTORE, CSV }
data class BackupUiState(val operation: BackupOperation? = null, val message: String? = null, val restored: Boolean = false)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: FinanceBackupRepository,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow(BackupUiState())
    val state = _state.asStateFlow()
    val pendingRestore = savedStateHandle.getStateFlow<String?>("backup_restore_uri", null)

    fun selectRestore(uri: Uri?) { if (_state.value.operation == null) savedStateHandle["backup_restore_uri"] = uri?.toString() }
    fun clearMessage() { _state.value = _state.value.copy(message = null, restored = false) }
    fun pickerFailed() { _state.value = BackupUiState(message = context.getString(R.string.backup_picker_error)) }

    fun confirmRestore() {
        val uri = pendingRestore.value ?: return
        savedStateHandle["backup_restore_uri"] = null
        run(BackupOperation.RESTORE, Uri.parse(uri))
    }

    fun run(operation: BackupOperation, uri: Uri) {
        if (_state.value.operation != null) return
        _state.value = BackupUiState(operation = operation)
        viewModelScope.launch {
            try {
                when (operation) {
                    BackupOperation.BACKUP -> repository.saveBackup(uri)
                    BackupOperation.RESTORE -> repository.restoreBackup(uri)
                    BackupOperation.CSV -> repository.saveCsv(uri)
                }
                val message = when (operation) {
                    BackupOperation.BACKUP -> R.string.backup_saved
                    BackupOperation.RESTORE -> R.string.backup_restored
                    BackupOperation.CSV -> R.string.backup_csv_saved
                }
                _state.value = BackupUiState(message = context.getString(message), restored = operation == BackupOperation.RESTORE)
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                val message = if (e is InvalidBackupException) {
                    context.getString(R.string.backup_invalid, e.message ?: context.getString(R.string.backup_invalid_fallback))
                } else context.getString(if (operation == BackupOperation.RESTORE) R.string.backup_restore_error else R.string.backup_write_error)
                _state.value = BackupUiState(message = message)
            }
        }
    }
}
