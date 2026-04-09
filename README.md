# hello-encrypted-shared-prefs

Modern encrypted key-value storage for Android, replacing the deprecated `EncryptedSharedPreferences`.

Built with **DataStore + Tink + Android Keystore**, with automatic hardware security module detection and fallback.

## Why this exists

`EncryptedSharedPreferences` from `androidx.security:security-crypto` was deprecated in 2025.
It never left alpha and shipped with serious issues:

- **Keyset corruption** on certain OEM devices (Samsung, Xiaomi), causing `AEADBadTagException` crashes
- **ANRs** from synchronous I/O combined with cryptographic operations on the main thread
- **No migration path** for upgrading encryption schemes
- **Abandoned** with no further patches planned

This project demonstrates the modern replacement stack that Google recommends.

## Architecture

```
+--------------------------------------------------+
|                    App Layer                      |
|  DemoViewModel  ->  EncryptedDataStore            |
+--------------------------------------------------+
         |                      |
    read/write            encrypt/decrypt
         |                      |
+------------------+   +-------------------+
|    DataStore     |   |    Tink AEAD      |
|  (Preferences)  |   |  (AES-256-GCM)    |
+------------------+   +-------------------+
                               |
                        master key stored in
                               |
                    +---------------------+
                    |  Android Keystore   |
                    +---------------------+
                        |          |
                  +---------+  +-------+
                  |StrongBox|  |  TEE  |
                  +---------+  +-------+
```

**Data flow:**

1. App writes a value through `EncryptedDataStore`
2. Tink encrypts the value using AES-256-GCM with a key from `AndroidKeysetManager`
3. The Tink keyset is itself encrypted by a master key in Android Keystore
4. The master key lives in the best available hardware: StrongBox or TEE
5. Encrypted data is persisted to DataStore (async, coroutine-based, no ANR risk)

## Security model

The app uses a **fallback chain** for key storage, always selecting the strongest option available:

| Level | Hardware | Examples | Tamper resistance |
|---|---|---|---|
| **StrongBox** | Dedicated secure element | Pixel Titan M2, Samsung Knox Vault | Highest: physically separate chip |
| **TEE** | Trusted Execution Environment | Most Android 8+ devices | High: isolated environment on the main SoC |
| **Software** | None | Emulators, old devices | Low: software-only keystore |

The app detects and reports which level it achieved at runtime.

### Hardware support

| Device | Secure hardware | StrongBox support | How it is accessed |
|---|---|---|---|
| Pixel 6+ | Titan M2 | Yes | Standard Android Keystore with `setIsStrongBoxBacked(true)` |
| Samsung Galaxy S21+ | Knox Vault | Yes | Standard Android Keystore with `setIsStrongBoxBacked(true)` |
| Most Android 8+ | TEE on SoC | No | Standard Android Keystore (default) |
| Emulators | None | No | Software keystore fallback |

No vendor-specific SDKs are needed. Both Titan M2 and Knox Vault expose themselves through the standard `StrongBox` API.

## Project structure

```
app/src/main/java/me/digitalby/helloencryptedsharedprefs/
  crypto/
    SecurityLevel.kt          # Enum: STRONGBOX, TEE, SOFTWARE
    SecurityLevelDetector.kt  # Queries device capabilities and verifies key backing
    KeyStoreManager.kt        # Master key generation with StrongBox/TEE fallback, Tink AEAD
    EncryptedDataStore.kt     # DataStore wrapper with Tink encrypt/decrypt
  ui/
    DemoScreen.kt             # Compose UI for read/write/delete and security level display
  DemoViewModel.kt            # ViewModel bridging crypto and UI
  MainActivity.kt             # Entry point
```

## Key dependencies

| Library | Purpose |
|---|---|
| `androidx.datastore:datastore-preferences` | Async key-value persistence (replaces SharedPreferences) |
| `com.google.crypto.tink:tink-android` | AEAD encryption with Android Keystore integration |
| Jetpack Compose + Material 3 | Demo UI |

## Building

```bash
git clone https://github.com/digitalby/hello-encrypted-shared-prefs.git
cd hello-encrypted-shared-prefs
./gradlew assembleDebug
```

Requires Android SDK with `compileSdk 35`. `minSdk` is 28 (Android 9), which is the minimum for StrongBox.

## How it compares to EncryptedSharedPreferences

| | EncryptedSharedPreferences | This approach |
|---|---|---|
| Storage | SharedPreferences (synchronous) | DataStore (async, coroutine-based) |
| Encryption | Tink via JetSec wrapper | Tink directly |
| Key management | `MasterKey` helper (brittle) | Direct Android Keystore + Tink keyset |
| StrongBox | Via `MasterKey.Builder` | Explicit, with fallback and reporting |
| Status | Deprecated, abandoned | Active, maintained components |
| ANR risk | High (sync I/O + crypto on main thread) | None (suspend functions) |

## References

- [EncryptedSharedPreferences deprecation](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences)
- [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- [Tink cryptographic library](https://github.com/tink-crypto/tink-java)
- [Android Keystore system](https://developer.android.com/privacy-and-security/keystore)
- [Hardware-backed Keystore (StrongBox)](https://developer.android.com/privacy-and-security/keystore#HardwareSecurityModule)

## License

MIT
