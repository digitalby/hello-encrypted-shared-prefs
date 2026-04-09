package me.digitalby.helloencryptedsharedprefs.crypto

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Base64

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "encrypted_prefs"
)

class EncryptedDataStore(
    private val context: Context,
    private val aead: Aead
) {

    fun read(key: String): Flow<String?> {
        val prefKey = stringPreferencesKey(key)
        return context.dataStore.data.map { preferences ->
            preferences[prefKey]?.let { encoded ->
                val ciphertext = Base64.getDecoder().decode(encoded)
                val associatedData = key.toByteArray(Charsets.UTF_8)
                String(aead.decrypt(ciphertext, associatedData), Charsets.UTF_8)
            }
        }
    }

    suspend fun write(key: String, value: String) {
        val prefKey = stringPreferencesKey(key)
        val associatedData = key.toByteArray(Charsets.UTF_8)
        val ciphertext = aead.encrypt(value.toByteArray(Charsets.UTF_8), associatedData)
        val encoded = Base64.getEncoder().encodeToString(ciphertext)
        context.dataStore.edit { preferences ->
            preferences[prefKey] = encoded
        }
    }

    suspend fun delete(key: String) {
        val prefKey = stringPreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences.remove(prefKey)
        }
    }

    fun allKeys(): Flow<Set<String>> =
        context.dataStore.data.map { preferences ->
            preferences.asMap().keys.map { it.name }.toSet()
        }
}
