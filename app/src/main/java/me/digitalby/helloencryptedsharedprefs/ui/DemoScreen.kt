package me.digitalby.helloencryptedsharedprefs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.digitalby.helloencryptedsharedprefs.DemoViewModel
import me.digitalby.helloencryptedsharedprefs.crypto.SecurityLevel

@Composable
fun DemoScreen(viewModel: DemoViewModel, modifier: Modifier = Modifier) {
    val securityLevel by viewModel.securityLevel.collectAsState()
    val readResult by viewModel.readResult.collectAsState()
    val storedKeys by viewModel.storedKeys.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val initError by viewModel.initError.collectAsState()

    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Encrypted DataStore Demo",
                style = MaterialTheme.typography.headlineMedium
            )

            SecurityLevelCard(securityLevel)

            initError?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Keystore Error",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text("Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Value") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { viewModel.write(key, value) },
                    enabled = key.isNotBlank() && value.isNotBlank() && !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
                OutlinedButton(
                    onClick = { viewModel.read(key) },
                    enabled = key.isNotBlank() && !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Load")
                }
                OutlinedButton(
                    onClick = { viewModel.delete(key) },
                    enabled = key.isNotBlank() && !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete")
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }

            readResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Decrypted Value",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = result,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (storedKeys.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Stored Keys",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        storedKeys.forEach { storedKey ->
                            Text(
                                text = storedKey,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityLevelCard(securityLevel: SecurityLevel?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (securityLevel) {
                SecurityLevel.STRONGBOX -> MaterialTheme.colorScheme.primaryContainer
                SecurityLevel.TEE -> MaterialTheme.colorScheme.tertiaryContainer
                SecurityLevel.SOFTWARE -> MaterialTheme.colorScheme.errorContainer
                null -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Security Level",
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = securityLevel?.displayName ?: "Initializing...",
                style = MaterialTheme.typography.titleLarge
            )
            securityLevel?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it.description,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
