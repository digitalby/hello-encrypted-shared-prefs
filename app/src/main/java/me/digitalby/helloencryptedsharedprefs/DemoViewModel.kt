package me.digitalby.helloencryptedsharedprefs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import me.digitalby.helloencryptedsharedprefs.crypto.EncryptedDataStore
import me.digitalby.helloencryptedsharedprefs.crypto.KeyStoreManager
import me.digitalby.helloencryptedsharedprefs.crypto.SecurityLevel

class DemoViewModel(application: Application) : AndroidViewModel(application) {

    private val keyStoreManager = KeyStoreManager(application)
    private val encryptedDataStore = EncryptedDataStore(application, keyStoreManager.getAead())

    private val _securityLevel = MutableStateFlow<SecurityLevel?>(null)
    val securityLevel: StateFlow<SecurityLevel?> = _securityLevel

    private val _readResult = MutableStateFlow<String?>(null)
    val readResult: StateFlow<String?> = _readResult

    private val _storedKeys = MutableStateFlow<Set<String>>(emptySet())
    val storedKeys: StateFlow<Set<String>> = _storedKeys

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        _securityLevel.value = keyStoreManager.securityLevel
        viewModelScope.launch {
            encryptedDataStore.allKeys().collect { keys ->
                _storedKeys.value = keys
            }
        }
    }

    fun write(key: String, value: String) {
        viewModelScope.launch {
            _isLoading.value = true
            encryptedDataStore.write(key, value)
            _readResult.value = value
            _isLoading.value = false
        }
    }

    fun read(key: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _readResult.value = encryptedDataStore.read(key).firstOrNull() ?: "(not found)"
            _isLoading.value = false
        }
    }

    fun delete(key: String) {
        viewModelScope.launch {
            _isLoading.value = true
            encryptedDataStore.delete(key)
            _readResult.value = null
            _isLoading.value = false
        }
    }
}
