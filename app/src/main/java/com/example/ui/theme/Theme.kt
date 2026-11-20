package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val CockpitDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF022C22),
    primaryContainer = EmeraldDeepContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = AmberSecondary,
    onSecondary = Color(0xFF291200),
    secondaryContainer = AmberContainer,
    onSecondaryContainer = AmberOnContainer,
    tertiary = CyanTertiary,
    onTertiary = Color(0xFF02202A),
    tertiaryContainer = CyanContainer,
    onTertiaryContainer = CyanOnContainer,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = MidnightSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateCard,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = MidnightSurface,
    surfaceContainer = SlateCard,
    surfaceContainerHigh = SlateCardElevated,
    surfaceContainerHighest = Color(0xFF263348),
    outline = SlateBorder,
    outlineVariant = Color(0xFF1F2A3E),
    error = CoralError,
    onError = Color.White,
    errorContainer = CoralContainer,
    onErrorContainer = CoralOnContainer
)

private val DaylightColorScheme = lightColorScheme(
    primary = ForestEmerald,
    onPrimary = Color.White,
    primaryContainer = EmeraldLightContainer,
    onPrimaryContainer = EmeraldOnLightContainer,
    secondary = WarmAmberDark,
    onSecondary = Color.White,
    secondaryContainer = AmberLightContainer,
    onSecondaryContainer = AmberOnLightContainer,
    tertiary = DeepCyan,
    onTertiary = Color.White,
    tertiaryContainer = CyanLightContainer,
    onTertiaryContainer = CyanOnLightContainer,
    background = AlabasterBg,
    onBackground = TextPrimaryLight,
    surface = PureWhiteSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = MistCard,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = PureWhiteSurface,
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = PureWhiteSurface,
    surfaceContainerHigh = MistCard,
    surfaceContainerHighest = Color(0xFFE2E8F0),
    outline = MistBorder,
    outlineVariant = Color(0xFFE2E8F0),
    error = CrimsonError,
    onError = Color.White,
    errorContainer = CrimsonLightContainer,
    onErrorContainer = CrimsonOnLightContainer
)

val SmartJobShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SmartJobHelperTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CockpitDarkColorScheme else DaylightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SmartJobTypography,
        shapes = SmartJobShapes,
        content = content
    )
}
