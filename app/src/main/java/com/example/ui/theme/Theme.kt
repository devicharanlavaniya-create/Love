package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class CampusStatusColors(
    val present: Color,
    val presentContainer: Color,
    val absent: Color,
    val absentContainer: Color,
    val late: Color,
    val lateContainer: Color,
    val excused: Color,
    val excusedContainer: Color
)

val LocalCampusStatusColors = staticCompositionLocalOf {
    CampusStatusColors(
        present = EmeraldPresent,
        presentContainer = EmeraldPresentSoft,
        absent = CrimsonAbsent,
        absentContainer = CrimsonAbsentSoft,
        late = AmberLate,
        lateContainer = AmberLateSoft,
        excused = IndigoExcused,
        excusedContainer = IndigoExcusedSoft
    )
}

private val LightColorScheme = lightColorScheme(
    primary = OxfordNavy,
    onPrimary = Color.White,
    primaryContainer = OxfordNavyContainer,
    onPrimaryContainer = OnOxfordNavyContainer,
    secondary = ScholarTeal,
    onSecondary = Color.White,
    secondaryContainer = ScholarTealContainer,
    onSecondaryContainer = OnScholarTealContainer,
    tertiary = AcademicAmber,
    onTertiary = Color.White,
    tertiaryContainer = AcademicAmberContainer,
    onTertiaryContainer = OnAcademicAmberContainer,
    background = IvoryCanvas,
    onBackground = InkPrimary,
    surface = WarmPaperSurface,
    onSurface = InkPrimary,
    surfaceVariant = SoftStoneSurface,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLow = Color(0xFFF6F4EE),
    surfaceContainer = SoftStoneSurface,
    surfaceContainerHigh = Color(0xFFEBE6DC),
    outline = CrispOutline,
    outlineVariant = CrispOutlineVariant,
    error = CrimsonAbsent,
    onError = Color.White,
    errorContainer = CrimsonAbsentSoft,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkSkyPrimary,
    onPrimary = Color(0xFF062038),
    primaryContainer = DarkSkyPrimaryContainer,
    onPrimaryContainer = Color(0xFFD6E4F0),
    secondary = DarkTealSecondary,
    onSecondary = Color(0xFF032D2D),
    secondaryContainer = DarkTealSecondaryContainer,
    onSecondaryContainer = Color(0xFFCDEBEB),
    tertiary = DarkAmberTertiary,
    onTertiary = Color(0xFF332000),
    tertiaryContainer = DarkAmberContainer,
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = DarkCanvas,
    onBackground = DarkInkPrimary,
    surface = DarkSurface,
    onSurface = DarkInkPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkInkSecondary,
    surfaceContainerLow = Color(0xFF101A25),
    surfaceContainer = DarkSurfaceElevated,
    surfaceContainerHigh = Color(0xFF213245),
    outline = DarkOutline,
    outlineVariant = Color(0xFF223142),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

private val CampusShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CampusPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val statusColors = if (darkTheme) {
        CampusStatusColors(
            present = Color(0xFF4ADE80),
            presentContainer = Color(0xFF113822),
            absent = Color(0xFFF87171),
            absentContainer = Color(0xFF451414),
            late = Color(0xFFFBBF24),
            lateContainer = Color(0xFF452A06),
            excused = Color(0xFFA5B4FC),
            excusedContainer = Color(0xFF1F2352)
        )
    } else {
        CampusStatusColors(
            present = EmeraldPresent,
            presentContainer = EmeraldPresentSoft,
            absent = CrimsonAbsent,
            absentContainer = CrimsonAbsentSoft,
            late = AmberLate,
            lateContainer = AmberLateSoft,
            excused = IndigoExcused,
            excusedContainer = IndigoExcusedSoft
        )
    }

    CompositionLocalProvider(LocalCampusStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CampusTypography,
            shapes = CampusShapes,
            content = content
        )
    }
}
