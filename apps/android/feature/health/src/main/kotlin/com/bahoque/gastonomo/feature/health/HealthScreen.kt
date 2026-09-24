package com.bahoque.gastonomo.feature.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bahoque.gastonomo.core.domain.health.ComponentHealth
import com.bahoque.gastonomo.core.domain.health.HealthReport
import com.bahoque.gastonomo.core.domain.health.HealthStatus
import com.bahoque.gastonomo.core.domain.health.ServiceVersion

private val SCREEN_PADDING = 16.dp
private val ITEM_SPACING = 8.dp

@Composable
fun HealthRoute(viewModel: HealthViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HealthScreen(state = state, onRefresh = viewModel::refresh, modifier = modifier)
}

@Composable
fun HealthScreen(state: HealthUiState, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(SCREEN_PADDING)
            .testTag(HealthScreenTags.ROOT),
        verticalArrangement = Arrangement.spacedBy(ITEM_SPACING),
    ) {
        Text(
            text = stringResource(R.string.health_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        when (state) {
            HealthUiState.Loading -> LoadingSection()
            is HealthUiState.Ready -> ReadySection(state.report)
            is HealthUiState.Failed -> FailedSection(state.reason)
        }
        Button(onClick = onRefresh, modifier = Modifier.testTag(HealthScreenTags.REFRESH)) {
            Text(text = stringResource(R.string.health_refresh))
        }
    }
}

@Composable
private fun LoadingSection() {
    CircularProgressIndicator(modifier = Modifier.testTag(HealthScreenTags.LOADING))
    Text(text = stringResource(R.string.health_loading))
}

@Composable
private fun ReadySection(report: HealthReport) {
    val summary = if (report.isHealthy) R.string.health_status_ok else R.string.health_status_degraded
    Text(
        text = stringResource(summary),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.testTag(HealthScreenTags.SUMMARY),
    )
    Text(text = stringResource(R.string.health_version, report.version.value))
    report.checks.forEach { ComponentRow(it) }
}

@Composable
private fun ComponentRow(component: ComponentHealth) {
    Text(
        text = stringResource(
            R.string.health_component,
            component.name,
            component.status.wireValue,
            component.latencyMs,
        ),
        modifier = Modifier.testTag(HealthScreenTags.COMPONENT),
    )
}

@Composable
private fun FailedSection(reason: HealthFailureReason) {
    val message = when (reason) {
        HealthFailureReason.UNREACHABLE -> R.string.health_error_unreachable
        HealthFailureReason.UNEXPECTED_RESPONSE -> R.string.health_error_unexpected
    }
    Text(
        text = stringResource(message),
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag(HealthScreenTags.ERROR),
    )
}

@Preview(showBackground = true)
@Composable
private fun HealthScreenReadyPreview() {
    HealthScreen(
        state = HealthUiState.Ready(
            HealthReport(
                version = ServiceVersion("0.1.0"),
                checks = listOf(ComponentHealth("database", HealthStatus.OK, 4)),
            ),
        ),
        onRefresh = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun HealthScreenFailedPreview() {
    HealthScreen(state = HealthUiState.Failed(HealthFailureReason.UNREACHABLE), onRefresh = {})
}
