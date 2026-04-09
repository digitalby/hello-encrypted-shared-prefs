package me.digitalby.helloencryptedsharedprefs.crypto

enum class SecurityLevel(val displayName: String, val description: String) {
    STRONGBOX(
        displayName = "StrongBox",
        description = "Keys stored in a dedicated secure element (Titan M2, Knox Vault, or equivalent)"
    ),
    TEE(
        displayName = "TEE",
        description = "Keys stored in the Trusted Execution Environment on the main SoC"
    ),
    SOFTWARE(
        displayName = "Software",
        description = "Keys stored in software-only keystore (no hardware backing)"
    )
}
