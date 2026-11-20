package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.formatCompactNumber
import com.example.engine.RuleEvaluator
import com.example.ui.MockRideOffer
import com.example.ui.SandboxDialogState
import com.example.ui.SandboxEvalPhase
import com.example.ui.SmartJobUiState
import com.example.ui.SmartJobViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SandboxScreen(
    uiState: SmartJobUiState,
    viewModel: SmartJobViewModel,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = uiState.config

    var customApp by remember { mutableStateOf("Rapido") }
    var customFare by remember { mutableStateOf("₹185") }
    var customDistance by remember { mutableStateOf("2.1 km") }
    var customButtonLabel by remember { mutableStateOf("Accept") }
    var customPickup by remember { mutableStateOf("Indiranagar 100ft Rd") }
    var customDrop by remember { mutableStateOf("Koramangala 4th Block") }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item(key = "sandbox_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Science,
                                    contentDescription = "Simulation Sandbox",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.sandbox_title),
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = stringResource(R.string.sandbox_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Active Rules Snapshot + Auto-Eval Toggle
            item(key = "active_rules_snapshot") {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Live Rule Auto-Evaluator",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Automatically parses spawned mock dialogs using your active Rules",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.sandboxAutoEvalEnabled,
                                onCheckedChange = { viewModel.setSandboxAutoEvalEnabled(it) },
                                modifier = Modifier.testTag("sandbox_autoeval_switch")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RulePillBadge(
                                label = if (config.minFareEnabled) {
                                    "Fare ≥ ₹${formatCompactNumber(config.minFareAmount)}"
                                } else {
                                    "Min Fare OFF"
                                },
                                active = config.minFareEnabled
                            )
                            RulePillBadge(
                                label = if (config.maxDistanceEnabled) {
                                    "Dist ≤ ${formatCompactNumber(config.maxDistanceKm)} km"
                                } else {
                                    "Max Dist OFF"
                                },
                                active = config.maxDistanceEnabled
                            )
                            RulePillBadge(
                                label = if (config.minPerKmEnabled) {
                                    "Rate ≥ ₹${formatCompactNumber(config.minPerKmRate)}/km"
                                } else {
                                    "₹/km OFF"
                                },
                                active = config.minPerKmEnabled
                            )
                            RulePillBadge(
                                label = if (config.skipIfUndetected) "Safe Mode ON" else "Keyword Fallback",
                                active = config.skipIfUndetected
                            )
                            RulePillBadge(
                                label = "Delay ${config.reactionDelayMs}ms",
                                active = true
                            )
                        }
                    }
                }
            }

            // Quick Scenario Spawners
            item(key = "quick_scenarios") {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "One-Tap Ride Offer Scenarios",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Spawn realistic ride-hailing popups to watch the rule engine parse, skip bad fares, and auto-click profitable offers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        ScenarioLaunchButton(
                            title = "Mock Low Fare: ₹55 • 3.8 km",
                            subtitle = "Rapido Bike • Tests skipping low fare & long distance",
                            accentColor = MaterialTheme.colorScheme.secondary,
                            tag = "mock_low_fare_button",
                            onClick = {
                                viewModel.spawnMockOffer(
                                    MockRideOffer(
                                        appName = "Rapido",
                                        packageName = "com.rapido.rider",
                                        pickupTitle = "MG Road Metro Gate 2",
                                        dropTitle = "Ulsoor Lake Main Rd",
                                        fareText = "₹55",
                                        distanceText = "3.8 km",
                                        buttonLabel = "Accept",
                                        extraNote = "Cash Ride • 12 min"
                                    )
                                )
                            }
                        )

                        ScenarioLaunchButton(
                            title = "Mock High Fare: ₹240 • 1.8 km",
                            subtitle = "Uber Driver • High payout, short pickup — should auto-click",
                            accentColor = MaterialTheme.colorScheme.primary,
                            tag = "mock_high_fare_button",
                            onClick = {
                                viewModel.spawnMockOffer(
                                    MockRideOffer(
                                        appName = "Uber Driver",
                                        packageName = "com.ubercab.driver",
                                        pickupTitle = "HSR Layout Sector 2",
                                        dropTitle = "Bellandur EcoSpace",
                                        fareText = "₹240",
                                        distanceText = "1.8 km",
                                        buttonLabel = "Accept",
                                        extraNote = "Uber Premier • 6 min"
                                    )
                                )
                            }
                        )

                        ScenarioLaunchButton(
                            title = "Mock Meter Pickup: Rs. 165 • 850 m",
                            subtitle = "Ola Driver • Tests Rs. currency & 850 m → 0.85 km conversion",
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            tag = "mock_meter_pickup_button",
                            onClick = {
                                viewModel.spawnMockOffer(
                                    MockRideOffer(
                                        appName = "Ola Driver",
                                        packageName = "com.olacabs.oladriver",
                                        pickupTitle = "BTM 2nd Stage",
                                        dropTitle = "Silk Board Flyover",
                                        fareText = "Rs. 165.00",
                                        distanceText = "850 m",
                                        buttonLabel = "Accept",
                                        extraNote = "Auto Priority • 4 min"
                                    )
                                )
                            }
                        )

                        ScenarioLaunchButton(
                            title = "Mock Undetected Metrics: Surge Mystery Order",
                            subtitle = "Porter • No ₹ or km shown — tests Safe Mode vs Keyword Fallback",
                            accentColor = MaterialTheme.colorScheme.outline,
                            tag = "mock_undetected_button",
                            onClick = {
                                viewModel.spawnMockOffer(
                                    MockRideOffer(
                                        appName = "Porter",
                                        packageName = "com.theporter.android.driverapp",
                                        pickupTitle = "Whitefield Industrial Hub",
                                        dropTitle = "Marathahalli Bridge",
                                        fareText = "Guaranteed Surge Bonus",
                                        distanceText = "Nearby Pickup • 5 min",
                                        buttonLabel = "Accept",
                                        extraNote = "Instant Payout"
                                    )
                                )
                            }
                        )
                    }
                }
            }

            // Custom Editable Test Card Builder
            item(key = "custom_builder") {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.sandbox_custom_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Test any currency format (₹, Rs., INR), unit (km, kms, m), or button keyword.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = customFare,
                                onValueChange = { customFare = it },
                                label = { Text("Fare Text") },
                                placeholder = { Text("₹185 or Rs. 210") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_fare_input")
                            )
                            OutlinedTextField(
                                value = customDistance,
                                onValueChange = { customDistance = it },
                                label = { Text("Distance Text") },
                                placeholder = { Text("2.1 km or 850 m") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_distance_input")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = customButtonLabel,
                                onValueChange = { customButtonLabel = it },
                                label = { Text("Button Label") },
                                placeholder = { Text("Accept / Confirm") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_button_label_input")
                            )
                            OutlinedTextField(
                                value = customApp,
                                onValueChange = { customApp = it },
                                label = { Text("App Label") },
                                placeholder = { Text("Swiggy / Zomato") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_app_input")
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.spawnMockOffer(
                                    MockRideOffer(
                                        appName = customApp.ifBlank { "Custom App" },
                                        packageName = "sandbox.simulation",
                                        pickupTitle = customPickup,
                                        dropTitle = customDrop,
                                        fareText = customFare,
                                        distanceText = customDistance,
                                        buttonLabel = customButtonLabel.ifBlank { "Accept" }
                                    )
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("spawn_custom_offer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Spawn Custom Ride Dialog",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // Recent Sandbox / Engine Evaluation Preview
            if (uiState.allHistory.isNotEmpty()) {
                item(key = "recent_eval_preview") {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Latest Evaluation",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                OutlinedButton(
                                    onClick = onNavigateToHistory,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("sandbox_view_history_button")
                                ) {
                                    Text("Full History (${uiState.allHistory.size})")
                                }
                            }
                            Text(
                                text = uiState.allHistory.first().summaryLine,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // Active Mock Ride-Request Dialog
    val activeDialog = uiState.sandboxDialog
    if (activeDialog != null) {
        MockRideRequestDialog(
            state = activeDialog,
            onAcceptClick = { viewModel.onSandboxAcceptTapped() },
            onDismiss = { viewModel.dismissSandboxDialog() }
        )
    }
}

@Composable
private fun ScenarioLaunchButton(
    title: String,
    subtitle: String,
    accentColor: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.TouchApp,
                contentDescription = "Spawn scenario",
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun RulePillBadge(label: String, active: Boolean) {
    Surface(
        color = if (active) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MockRideRequestDialog(
    state: SandboxDialogState,
    onAcceptClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val offer = state.offer

    val bannerBg by animateColorAsState(
        targetValue = when (state.phase) {
            SandboxEvalPhase.AUTO_CLICKED, SandboxEvalPhase.MANUAL_ACCEPTED ->
                MaterialTheme.colorScheme.primaryContainer
            SandboxEvalPhase.SKIPPED ->
                MaterialTheme.colorScheme.secondaryContainer
            SandboxEvalPhase.NOT_DETECTED ->
                MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        label = "dialog_banner_bg"
    )

    val bannerTextColor by animateColorAsState(
        targetValue = when (state.phase) {
            SandboxEvalPhase.AUTO_CLICKED, SandboxEvalPhase.MANUAL_ACCEPTED ->
                MaterialTheme.colorScheme.onPrimaryContainer
            SandboxEvalPhase.SKIPPED ->
                MaterialTheme.colorScheme.onSecondaryContainer
            SandboxEvalPhase.NOT_DETECTED ->
                MaterialTheme.colorScheme.onTertiaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "dialog_banner_fg"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(
                width = 1.5.dp,
                color = when (state.phase) {
                    SandboxEvalPhase.AUTO_CLICKED -> MaterialTheme.colorScheme.primary
                    SandboxEvalPhase.SKIPPED -> MaterialTheme.colorScheme.secondary
                    SandboxEvalPhase.NOT_DETECTED -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.outline
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .widthIn(max = 460.dp)
                .testTag("mock_ride_dialog")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Live Engine Telemetry Banner
                Surface(
                    color = bannerBg,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (state.phase) {
                            SandboxEvalPhase.SCANNING -> {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            SandboxEvalPhase.AUTO_CLICKED, SandboxEvalPhase.MANUAL_ACCEPTED -> {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            SandboxEvalPhase.SKIPPED -> {
                                Icon(
                                    imageVector = Icons.Rounded.Block,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            SandboxEvalPhase.NOT_DETECTED -> {
                                Icon(
                                    imageVector = Icons.Rounded.HelpOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Rounded.LocalTaxi,
                                    contentDescription = null,
                                    tint = bannerTextColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = state.statusBanner,
                            style = MaterialTheme.typography.labelMedium,
                            color = bannerTextColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Simulated Incoming Ride Popup Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalTaxi,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "New ${offer.appName} Request",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = offer.extraNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("mock_dialog_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close mock dialog"
                        )
                    }
                }

                // Fare & Distance Highlight Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ESTIMATED FARE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = offer.fareText,
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "DISTANCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = offer.distanceText,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                // Pickup & Drop Route
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Pickup: ${offer.pickupTitle}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Drop: ${offer.dropTitle}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Parsed Telemetry Breakdown
                AnimatedVisibility(visible = state.evaluation != null) {
                    val eval = state.evaluation
                    if (eval != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                text = "PARSED BY ENGINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                RulePillBadge(
                                    label = eval.fareAmount?.let { "Fare: ₹${formatCompactNumber(it)}" } ?: "Fare: --",
                                    active = eval.fareAmount != null
                                )
                                RulePillBadge(
                                    label = eval.distanceKm?.let { "Dist: ${RuleEvaluator.formatDistance(it)}" } ?: "Dist: --",
                                    active = eval.distanceKm != null
                                )
                                if (eval.ratePerKm != null) {
                                    RulePillBadge(
                                        label = "₹${formatCompactNumber(eval.ratePerKm)}/km",
                                        active = true
                                    )
                                }
                                eval.triggeredRules.take(3).forEach { rule ->
                                    RulePillBadge(label = rule, active = eval.shouldClick)
                                }
                            }
                        }
                    }
                }

                // Action Buttons (Decline & Target Accept Button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("mock_dialog_decline_button")
                    ) {
                        Text(
                            text = if (state.phase == SandboxEvalPhase.AUTO_CLICKED ||
                                state.phase == SandboxEvalPhase.MANUAL_ACCEPTED
                            ) "Close" else "Decline"
                        )
                    }

                    Button(
                        onClick = onAcceptClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.phase == SandboxEvalPhase.AUTO_CLICKED ||
                                state.phase == SandboxEvalPhase.MANUAL_ACCEPTED
                            ) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            contentColor = if (state.phase == SandboxEvalPhase.AUTO_CLICKED ||
                                state.phase == SandboxEvalPhase.MANUAL_ACCEPTED
                            ) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            }
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .testTag("mock_dialog_accept_button")
                    ) {
                        Text(
                            text = if (state.phase == SandboxEvalPhase.AUTO_CLICKED) {
                                "✓ ${offer.buttonLabel}ed"
                            } else {
                                offer.buttonLabel
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
