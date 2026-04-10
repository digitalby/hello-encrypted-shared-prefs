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
    private var encryptedDataStore: EncryptedDataStore? = null

    private val _securityLevel = MutableStateFlow<SecurityLevel?>(null)
    val securityLevel: StateFlow<SecurityLevel?> = _securityLevel

    private val _readResult = MutableStateFlow<String?>(null)
    val readResult: StateFlow<String?> = _readResult

    private val _storedKeys = MutableStateFlow<Set<String>>(emptySet())
    val storedKeys: StateFlow<Set<String>> = _storedKeys

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _initError = MutableStateFlow<String?>(null)
    val initError: StateFlow<String?> = _initError

    init {
        viewModelScope.launch {
            try {
                keyStoreManager.initialize()
                val store = EncryptedDataStore(application, keyStoreManager.getAead())
                encryptedDataStore = store
                _securityLevel.value = keyStoreManager.securityLevel
                if (keyStoreManager.didResetKeys) {
                    store.clear()
                    _initError.value = "Encryption keys were invalidated. Stored data has been cleared."
                }
                store.allKeys().collect { keys ->
                    _storedKeys.value = keys
                }
            } catch (e: Exception) {
                _initError.value = e.message ?: "Keystore initialization failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun write(key: String, value: String) {
        val store = encryptedDataStore ?: return
        viewModelScope.launch {
            _isLoading.value = true
            store.write(key, value)
            _readResult.value = value
            _isLoading.value = false
        }
    }

    fun read(key: String) {
        val store = encryptedDataStore ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _readResult.value = store.read(key).firstOrNull() ?: "(not found)"
            _isLoading.value = false
        }
    }

    fun delete(key: String) {
        val store = encryptedDataStore ?: return
        viewModelScope.launch {
            _isLoading.value = true
            store.delete(key)
            _readResult.value = null
            _isLoading.value = false
        }
    }
}
