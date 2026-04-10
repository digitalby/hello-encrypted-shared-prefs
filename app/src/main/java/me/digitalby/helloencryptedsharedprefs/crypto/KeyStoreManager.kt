package me.digitalby.helloencryptedsharedprefs.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Log
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import java.security.KeyStoreException
import javax.crypto.KeyGenerator

class KeyStoreManager(private val context: Context) {

    companion object {
        private const val TAG = "KeyStoreManager"
        private const val KEYSTORE_ALIAS = "hello_esp_master_key"
        private const val TINK_KEYSET_NAME = "hello_esp_tink_keyset"
        private const val TINK_PREF_FILE = "hello_esp_tink_prefs"
        private const val KEYSTORE_URI = "android-keystore://$KEYSTORE_ALIAS"
    }

    var securityLevel: SecurityLevel = SecurityLevel.SOFTWARE
        private set

    var didResetKeys: Boolean = false
        private set

    suspend fun initialize() {
        withContext(Dispatchers.Default) {
            AeadConfig.register()
            ensureAndroidKeyStoreKey()
            securityLevel = SecurityLevelDetector.detectSecurityLevel(KEYSTORE_ALIAS)
        }
    }

    fun getAead(): Aead {
        val keysetManager = AndroidKeysetManager.Builder()
            .withSharedPref(context, TINK_KEYSET_NAME, TINK_PREF_FILE)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(KEYSTORE_URI)
            .build()

        return keysetManager.keysetHandle.getPrimitive(Aead::class.java)
    }

    private fun ensureAndroidKeyStoreKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            if (isKeyUsable(keyStore)) return
            Log.w(TAG, "Existing key is unusable, resetting keystore and Tink keyset")
            resetKeys(keyStore)
        }

        generateNewKey()
    }

    private fun isKeyUsable(keyStore: KeyStore): Boolean {
        return try {
            val entry = keyStore.getEntry(KEYSTORE_ALIAS, null)
                as? KeyStore.SecretKeyEntry ?: return false
            entry.secretKey.encoded
            true
        } catch (_: KeyPermanentlyInvalidatedException) {
            false
        } catch (_: KeyStoreException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun resetKeys(keyStore: KeyStore) {
        didResetKeys = true
        try {
            keyStore.deleteEntry(KEYSTORE_ALIAS)
        } catch (e: KeyStoreException) {
            Log.e(TAG, "Failed to delete corrupted key", e)
        }
        context.getSharedPreferences(TINK_PREF_FILE, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    private fun generateNewKey() {
        val specBuilder = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)

        try {
            specBuilder.setIsStrongBoxBacked(true)
            generateKey(specBuilder.build())
        } catch (_: StrongBoxUnavailableException) {
            specBuilder.setIsStrongBoxBacked(false)
            generateKey(specBuilder.build())
        }
    }

    private fun generateKey(spec: KeyGenParameterSpec) {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        keyGenerator.init(spec)
        keyGenerator.generateKey()
    }
}
