package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppPreset
import com.example.data.model.PermissionStatus
import com.example.data.model.RuleConfig
import com.example.data.model.formatCompactNumber
import com.example.ui.SmartJobUiState
import com.example.ui.SmartJobViewModel
import com.example.util.PermissionHelper
import kotlin.math.roundToLong

@Composable
fun DashboardScreen(
    uiState: SmartJobUiState,
    viewModel: SmartJobViewModel,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config = uiState.config

    // Local string drafts for numeric inputs so typing decimals feels effortless
    var minFareText by remember { mutableStateOf(formatCompactNumber(config.minFareAmount)) }
    var maxDistanceText by remember { mutableStateOf(formatCompactNumber(config.maxDistanceKm)) }
    var minPerKmText by remember { mutableStateOf(formatCompactNumber(config.minPerKmRate)) }
    var packageText by remember { mutableStateOf(config.targetPackageName) }
    var keywordsText by remember { mutableStateOf(config.actionKeywords) }

    LaunchedEffect(config.selectedPresetId, config.targetPackageName) {
        if (packageText != config.targetPackageName) {
            packageText = config.targetPackageName
        }
    }
    LaunchedEffect(config.selectedPresetId, config.actionKeywords) {
        if (keywordsText != config.actionKeywords) {
            keywordsText = config.actionKeywords
        }
    }
    LaunchedEffect(config.minFareAmount) {
        val parsed = minFareText.toDoubleOrNull()
        if (parsed == null || parsed != config.minFareAmount) {
            minFareText = formatCompactNumber(config.minFareAmount)
        }
    }
    LaunchedEffect(config.maxDistanceKm) {
        val parsed = maxDistanceText.toDoubleOrNull()
        if (parsed == null || parsed != config.maxDistanceKm) {
            maxDistanceText = formatCompactNumber(config.maxDistanceKm)
        }
    }
    LaunchedEffect(config.minPerKmRate) {
        val parsed = minPerKmText.toDoubleOrNull()
        if (parsed == null || parsed != config.minPerKmRate) {
            minPerKmText = formatCompactNumber(config.minPerKmRate)
        }
    }

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
            // 1. Top App Title Bar + Theme Toggle
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = "SmartJobHelper emblem",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Gig Driver Smart Screen Assistant",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.toggleDarkTheme() },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (config.darkThemeEnabled) {
                                Icons.Outlined.LightMode
                            } else {
                                Icons.Outlined.DarkMode
                            },
                            contentDescription = "Toggle light or dark cockpit theme",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Hero Live Status & Floating Overlay Control Banner
            item(key = "hero_status") {
                CockpitStatusHeroCard(
                    isActive = uiState.isActive,
                    isOverlayRunning = uiState.isOverlayRunning,
                    isAccessibilityConnected = uiState.isAccessibilityConnected,
                    activeRuleTag = config.activePillRuleTag(),
                    acceptedCount = uiState.acceptedCount,
                    skippedCount = uiState.skippedCount,
                    onToggleActive = { viewModel.toggleAssistantActive() },
                    onToggleOverlay = { viewModel.startOrStopOverlay() }
                )
            }

            // 3. 3-Step Permissions Setup Wizard
            item(key = "permissions_wizard") {
                PermissionsSetupCard(
                    permissions = uiState.permissions,
                    onOpenAccessibility = { PermissionHelper.openAccessibilitySettings(context) },
                    onOpenOverlay = { PermissionHelper.openOverlaySettings(context) },
                    onOpenBattery = { PermissionHelper.openBatteryOptimizationSettings(context) }
                )
            }

            // 4. Quick App Preset Chips + Target Package & Keywords
            item(key = "presets_and_target") {
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
                        SectionHeaderRow(
                            icon = Icons.Rounded.Tune,
                            title = stringResource(R.string.dashboard_presets_title),
                            subtitle = "One-tap package & keyword autofill"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppPreset.entries.forEach { preset ->
                                val selected = config.selectedPresetId == preset.id
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.selectPreset(preset) },
                                    label = {
                                        Text(
                                            text = preset.displayName,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    },
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("preset_chip_${preset.id}")
                                )
                            }
                        }

                        OutlinedTextField(
                            value = packageText,
                            onValueChange = {
                                packageText = it
                                viewModel.updateTargetPackage(it)
                            },
                            label = { Text(stringResource(R.string.dashboard_target_package_label)) },
                            placeholder = { Text(stringResource(R.string.dashboard_target_package_hint)) },
                            supportingText = {
                                Text(
                                    text = if (packageText.isBlank()) {
                                        "Monitoring ALL active apps (Custom / All mode)"
                                    } else {
                                        "Scoped to ${AppPreset.labelForPackage(packageText)}"
                                    }
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("target_package_input")
                        )

                        OutlinedTextField(
                            value = keywordsText,
                            onValueChange = {
                                keywordsText = it
                                viewModel.updateActionKeywords(it)
                            },
                            label = { Text(stringResource(R.string.dashboard_keywords_label)) },
                            placeholder = { Text("Accept, Confirm, Pick Up") },
                            supportingText = {
                                Text(stringResource(R.string.dashboard_keywords_hint))
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("action_keywords_input")
                        )
                    }
                }
            }

            // 5. Financial & Distance Filter Rules
            item(key = "financial_rules") {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SectionHeaderRow(
                            icon = Icons.Rounded.CurrencyRupee,
                            title = stringResource(R.string.dashboard_rules_title),
                            subtitle = "All enabled thresholds must pass before clicking"
                        )

                        // Minimum Fare Rule
                        RuleNumericRow(
                            title = stringResource(R.string.dashboard_min_fare_label),
                            prefix = "₹",
                            suffix = "min",
                            enabled = config.minFareEnabled,
                            onEnabledChange = { viewModel.updateMinFareEnabled(it) },
                            valueText = minFareText,
                            onValueChange = { raw ->
                                val filtered = raw.filter { it.isDigit() || it == '.' }
                                minFareText = filtered
                                filtered.toDoubleOrNull()?.let { viewModel.updateMinFareAmount(it) }
                            },
                            icon = Icons.Rounded.CurrencyRupee,
                            switchTag = "min_fare_toggle",
                            inputTag = "min_fare_input"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Maximum Distance Rule
                        RuleNumericRow(
                            title = stringResource(R.string.dashboard_max_distance_label),
                            prefix = "≤",
                            suffix = "km",
                            enabled = config.maxDistanceEnabled,
                            onEnabledChange = { viewModel.updateMaxDistanceEnabled(it) },
                            valueText = maxDistanceText,
                            onValueChange = { raw ->
                                val filtered = raw.filter { it.isDigit() || it == '.' }
                                maxDistanceText = filtered
                                filtered.toDoubleOrNull()?.let { viewModel.updateMaxDistanceKm(it) }
                            },
                            icon = Icons.Rounded.Route,
                            switchTag = "max_distance_toggle",
                            inputTag = "max_distance_input"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Minimum Per-KM Rate Rule
                        RuleNumericRow(
                            title = stringResource(R.string.dashboard_min_per_km_label),
                            prefix = "₹",
                            suffix = "/km",
                            enabled = config.minPerKmEnabled,
                            onEnabledChange = { viewModel.updateMinPerKmEnabled(it) },
                            valueText = minPerKmText,
                            onValueChange = { raw ->
                                val filtered = raw.filter { it.isDigit() || it == '.' }
                                minPerKmText = filtered
                                filtered.toDoubleOrNull()?.let { viewModel.updateMinPerKmRate(it) }
                            },
                            icon = Icons.Rounded.Speed,
                            switchTag = "min_per_km_toggle",
                            inputTag = "min_per_km_input"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Safe Mode / Fallback Behavior Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = if (config.skipIfUndetected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (config.skipIfUndetected) {
                                            stringResource(R.string.dashboard_safe_mode_title)
                                        } else {
                                            "Keyword-Only Fallback"
                                        },
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = if (config.skipIfUndetected) {
                                            stringResource(R.string.dashboard_safe_mode_desc)
                                        } else {
                                            stringResource(R.string.dashboard_keyword_fallback_desc)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = config.skipIfUndetected,
                                onCheckedChange = { viewModel.updateSkipIfUndetected(it) },
                                modifier = Modifier.testTag("safe_mode_toggle")
                            )
                        }
                    }
                }
            }

            // 6. Click Speed & Safety Controls
            item(key = "speed_controls") {
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
                        SectionHeaderRow(
                            icon = Icons.Rounded.Timer,
                            title = stringResource(R.string.dashboard_speed_title),
                            subtitle = "Fine-tune response timing and anti-double-click guard"
                        )

                        // Reaction Delay Slider (0 - 1000 ms)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.dashboard_reaction_delay),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (config.reactionDelayMs == 0L) {
                                            "0 ms (Instant)"
                                        } else {
                                            "${config.reactionDelayMs} ms"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Slider(
                                value = config.reactionDelayMs.toFloat(),
                                onValueChange = { raw ->
                                    val snapped = ((raw / 50f).roundToLong() * 50L).coerceIn(0L, 1000L)
                                    viewModel.updateReactionDelayMs(snapped)
                                },
                                valueRange = 0f..1000f,
                                steps = 19,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.tertiary,
                                    activeTrackColor = MaterialTheme.colorScheme.tertiary
                                ),
                                modifier = Modifier.testTag("reaction_delay_slider")
                            )
                        }

                        // Cooldown / Debounce Timer Slider (500 - 5000 ms)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.dashboard_cooldown_timer),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${config.cooldownMs} ms",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Slider(
                                value = config.cooldownMs.toFloat(),
                                onValueChange = { raw ->
                                    val snapped = ((raw / 250f).roundToLong() * 250L).coerceIn(500L, 5000L)
                                    viewModel.updateCooldownMs(snapped)
                                },
                                valueRange = 500f..5000f,
                                steps = 17,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.secondary,
                                    activeTrackColor = MaterialTheme.colorScheme.secondary
                                ),
                                modifier = Modifier.testTag("cooldown_slider")
                            )
                        }
                    }
                }
            }

            // 7. Live Activity Log Strip linking to full Job History Screen
            item(key = "live_activity_strip") {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToHistory() }
                        .testTag("live_log_banner_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LIVE ACTIVITY FEED",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = uiState.lastSummary.ifBlank { "No evaluated popups yet" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "Open full Job History",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun CockpitStatusHeroCard(
    isActive: Boolean,
    isOverlayRunning: Boolean,
    isAccessibilityConnected: Boolean,
    activeRuleTag: String,
    acceptedCount: Int,
    skippedCount: Int,
    onToggleActive: () -> Unit,
    onToggleOverlay: () -> Unit
) {
    val accentColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "status_accent"
    )

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.5.dp, accentColor),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1220)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_cockpit_banner_1795209841966),
                contentDescription = "Radar route cockpit backdrop",
                contentScale = ContentScale.Crop,
                alpha = 0.55f,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xEE090E17),
                                Color(0xCC090E17),
                                Color(0x66090E17)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status pill mirroring the floating overlay badge
                    Surface(
                        color = if (isActive) Color(0xFF059669) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isActive) Color(0xFF6EE7B7) else Color(0xFF475569)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isActive) Color(0xFFA7F3D0) else Color(0xFF94A3B8)
                                    )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isActive) "ACTIVE ($activeRuleTag)" else "PAUSED",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeroMetricChip(label = "Accepted", value = "$acceptedCount", tint = Color(0xFF34D399))
                        HeroMetricChip(label = "Skipped", value = "$skippedCount", tint = Color(0xFFFBBF24))
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isActive) "Auto-Evaluator Armed" else "Engine Standing By",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = when {
                            !isAccessibilityConnected -> "Accessibility service not yet enabled — grant below or test in Sandbox."
                            isActive -> "Watching popups in real time and clicking only profitable offers."
                            else -> "Starts paused for safety. Tap Activate or tap the floating pill to begin."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onToggleActive,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive) Color(0xFF334155) else Color(0xFF10B981),
                            contentColor = if (isActive) Color.White else Color(0xFF022C22)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("toggle_assistant_button")
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Rounded.PauseCircle else Icons.Rounded.PlayCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isActive) "Pause Engine" else "Activate",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    OutlinedButton(
                        onClick = onToggleOverlay,
                        border = BorderStroke(
                            1.dp,
                            if (isOverlayRunning) Color(0xFF34D399) else Color(0xFF64748B)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isOverlayRunning) Color(0x3310B981) else Color(0x440F172A),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("toggle_overlay_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PictureInPictureAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isOverlayRunning) "Stop Pill" else "Launch Pill",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMetricChip(label: String, value: String, tint: Color) {
    Surface(
        color = Color(0xAA0F172A),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = tint,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun PermissionsSetupCard(
    permissions: PermissionStatus,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenBattery: () -> Unit
) {
    var expanded by remember(permissions.allCoreGranted) {
        mutableStateOf(!permissions.allCoreGranted)
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (permissions.allCoreGranted) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .testTag("permissions_header_row"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (permissions.allCoreGranted) {
                            Icons.Rounded.CheckCircle
                        } else {
                            Icons.Rounded.WarningAmber
                        },
                        contentDescription = null,
                        tint = if (permissions.allCoreGranted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.perm_wizard_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${permissions.grantedCoreCount} of 3 system permissions ready",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Collapse checklist" else "Expand checklist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PermissionStepRow(
                        stepNumber = "1",
                        icon = Icons.Outlined.AccessibilityNew,
                        title = stringResource(R.string.perm_accessibility_title),
                        description = stringResource(R.string.perm_accessibility_desc),
                        granted = permissions.accessibilityEnabled,
                        onGrantClick = onOpenAccessibility,
                        tag = "perm_accessibility_button"
                    )
                    PermissionStepRow(
                        stepNumber = "2",
                        icon = Icons.Outlined.Layers,
                        title = stringResource(R.string.perm_overlay_title),
                        description = stringResource(R.string.perm_overlay_desc),
                        granted = permissions.overlayEnabled,
                        onGrantClick = onOpenOverlay,
                        tag = "perm_overlay_button"
                    )
                    PermissionStepRow(
                        stepNumber = "3",
                        icon = Icons.Outlined.BatteryChargingFull,
                        title = stringResource(R.string.perm_battery_title),
                        description = stringResource(R.string.perm_battery_desc),
                        granted = permissions.batteryOptimizationIgnored,
                        onGrantClick = onOpenBattery,
                        tag = "perm_battery_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionStepRow(
    stepNumber: String,
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    onGrantClick: () -> Unit,
    tag: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onGrantClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (granted) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                shape = CircleShape,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (granted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$stepNumber. $title",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                color = if (granted) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (granted) {
                        stringResource(R.string.perm_status_granted)
                    } else {
                        stringResource(R.string.perm_status_missing)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (granted) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun RuleNumericRow(
    title: String,
    prefix: String,
    suffix: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    valueText: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    switchTag: String,
    inputTag: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                modifier = Modifier.testTag(switchTag)
            )
        }

        AnimatedVisibility(visible = enabled) {
            OutlinedTextField(
                value = valueText,
                onValueChange = onValueChange,
                prefix = {
                    Text(
                        text = "$prefix ",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                suffix = {
                    Text(
                        text = suffix,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(inputTag)
            )
        }
    }
}

@Composable
private fun SectionHeaderRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
