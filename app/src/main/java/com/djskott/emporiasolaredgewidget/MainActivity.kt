package com.djskott.emporiasolaredgewidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import com.djskott.emporiasolaredgewidget.data.SecureConfigRepository
import com.djskott.emporiasolaredgewidget.data.WidgetSnapshotStore
import com.djskott.emporiasolaredgewidget.domain.EnergySnapshotService
import com.djskott.emporiasolaredgewidget.model.WidgetConfig
import com.djskott.emporiasolaredgewidget.model.WidgetSnapshot
import com.djskott.emporiasolaredgewidget.widget.EmporiaSolarEdgeWidget
import com.djskott.emporiasolaredgewidget.widget.WidgetRefreshScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ConfigScreen()
                }
            }
        }
    }
}

@Composable
private fun ConfigScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val configRepository = remember { SecureConfigRepository(context) }
    val snapshotStore = remember { WidgetSnapshotStore(context) }
    val scope = rememberCoroutineScope()

    var emporiaEmail by rememberSaveable { mutableStateOf("") }
    var emporiaPassword by rememberSaveable { mutableStateOf("") }
    var solarEdgeApiKey by rememberSaveable { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("Loading saved settings...") }
    var discoverySummary by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        WidgetRefreshScheduler.ensurePeriodic(context)
        val config = withContext(Dispatchers.IO) { configRepository.load() }
        emporiaEmail = config?.emporiaEmail.orEmpty()
        emporiaPassword = config?.emporiaPassword.orEmpty()
        solarEdgeApiKey = config?.solarEdgeApiKey.orEmpty()
        statusMessage = if (config?.hasCredentials == true) {
            "Saved settings found. Tap Save and Refresh to validate again."
        } else {
            "Enter your Emporia login and SolarEdge API key. The app auto-detects your Vue mains and first SolarEdge site."
        }
        if (!config?.solarEdgeSiteId.isNullOrBlank() || !config?.emporiaDeviceId.isNullOrBlank()) {
            discoverySummary = buildString {
                append("Saved SolarEdge site: ${config?.solarEdgeSiteId ?: "--"}")
                append(" | Emporia device: ${config?.emporiaDeviceId ?: "--"}")
                config?.emporiaCircuitIds
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { append(" | Circuits: ${it.joinToString(", ")}") }
            }
        }
        isBusy = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Emporia Solar Edge Widget",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "This app stores your Emporia login and SolarEdge key locally on the device, refreshes the widget every 15 minutes, and lets you manually refresh at any time from the widget.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = emporiaEmail,
            onValueChange = { emporiaEmail = it },
            label = { Text("Emporia email") },
            singleLine = true,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        OutlinedTextField(
            value = emporiaPassword,
            onValueChange = { emporiaPassword = it },
            label = { Text("Emporia password") },
            singleLine = true,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
        )
        OutlinedTextField(
            value = solarEdgeApiKey,
            onValueChange = { solarEdgeApiKey = it },
            label = { Text("SolarEdge API key") },
            singleLine = true,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = {
                    scope.launch {
                        isBusy = true
                        statusMessage = "Connecting to Emporia and SolarEdge..."
                        discoverySummary = ""
                        try {
                            val refreshResult = withContext(Dispatchers.IO) {
                                EnergySnapshotService(context).refresh(
                                    WidgetConfig(
                                        emporiaEmail = emporiaEmail.trim(),
                                        emporiaPassword = emporiaPassword,
                                        solarEdgeApiKey = solarEdgeApiKey.trim(),
                                    ),
                                )
                            }
                            withContext(Dispatchers.IO) {
                                configRepository.save(refreshResult.resolvedConfig)
                                snapshotStore.save(refreshResult.snapshot)
                            }
                            WidgetRefreshScheduler.ensurePeriodic(context)
                            EmporiaSolarEdgeWidget().updateAll(context)
                            discoverySummary = buildString {
                                append("SolarEdge site: ${refreshResult.resolvedConfig.solarEdgeSiteId}")
                                append(" | Emporia device: ${refreshResult.resolvedConfig.emporiaDeviceId}")
                                append(" | Circuits: ${refreshResult.resolvedConfig.emporiaCircuitIds.joinToString(", ")}")
                            }
                            statusMessage = "Saved and refreshed successfully."
                        } catch (error: Exception) {
                            statusMessage = error.message ?: "Unable to validate the configuration."
                        } finally {
                            isBusy = false
                        }
                    }
                },
                enabled = !isBusy &&
                    emporiaEmail.isNotBlank() &&
                    emporiaPassword.isNotBlank() &&
                    solarEdgeApiKey.isNotBlank(),
            ) {
                Text(if (isBusy) "Working..." else "Save and Refresh")
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            configRepository.clear()
                            snapshotStore.save(WidgetSnapshot.notConfigured())
                        }
                        EmporiaSolarEdgeWidget().updateAll(context)
                        emporiaEmail = ""
                        emporiaPassword = ""
                        solarEdgeApiKey = ""
                        discoverySummary = ""
                        statusMessage = "Stored credentials cleared."
                    }
                },
                enabled = !isBusy,
            ) {
                Text("Clear")
            }
        }

        if (discoverySummary.isNotBlank()) {
            Text(
                text = discoverySummary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
