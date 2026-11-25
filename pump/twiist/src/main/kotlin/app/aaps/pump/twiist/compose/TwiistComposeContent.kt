package app.aaps.pump.twiist.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.aaps.core.ui.compose.ComposablePluginContent
import app.aaps.core.ui.compose.ToolbarConfig
import app.aaps.pump.twiist.R
import app.aaps.core.ui.R as CoreUiR

/** Minimal overview for the skeleton: makes the locked state impossible to miss. */
class TwiistComposeContent(
    private val pluginName: String,
    private val transportIsEmulator: Boolean
) : ComposablePluginContent {

    @Composable
    override fun Render(
        setToolbarConfig: (ToolbarConfig) -> Unit,
        onNavigateBack: () -> Unit,
        onSettings: (() -> Unit)?
    ) {
        LaunchedEffect(Unit) {
            setToolbarConfig(
                ToolbarConfig(
                    title = pluginName,
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(CoreUiR.string.back))
                        }
                    },
                    actions = {}
                )
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.twiist_status_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.twiist_status_locked), color = MaterialTheme.colorScheme.error)
            Text(stringResource(if (transportIsEmulator) R.string.twiist_transport_emulator else R.string.twiist_transport_none))
        }
    }
}
