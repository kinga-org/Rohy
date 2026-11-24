package app.rohy

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Colors of the design board "Rohy Android — refonte" (palette Lagon & Soleil), one set per appearance. */
data class RohyColors(
    val ground: Color,
    val surface: Color,
    val line: Color,
    val divider: Color,
    val controlLine: Color,
    val ink: Color,
    val muted: Color,
    val primary: Color,
    val onPrimary: Color,
    val accentText: Color,
    /** Soft lagoon: icon tiles, tonal buttons. */
    val accentSoft: Color,
    val onAccentSoft: Color,
    /** Sun yellow, for small cheerful details (heart, cheeks, some icon tiles). */
    val sun: Color,
    val sunSoft: Color,
    val onSunSoft: Color,
    val neutralSoft: Color,
    /** Connection card on the home screen, its text, and the smiling devices drawn on it. */
    val heroCard: Color,
    val onHeroCard: Color,
    val heroDevice: Color,
    val success: Color,
    val successText: Color,
    val successSoft: Color,
    val offline: Color,
    val warning: Color,
    val warningText: Color,
    val warningSoft: Color,
    val danger: Color,
    val disabled: Color,
    val disabledText: Color,
)

/** Lagoon of the app icon ("La corde"): the same in both appearances. */
val Brand = Color(0xFF1B6E85)

/** Sun yellow of the rope's ends in the app icon. */
val BrandSun = Color(0xFFF5B71E)

val LightColors = RohyColors(
    ground = Color(0xFFF4F9FA),
    surface = Color.White,
    line = Color(0xFFE0ECEF),
    divider = Color(0xFFE6F0F2),
    controlLine = Color(0xFFC7DADF),
    ink = Color(0xFF0E2A33),
    muted = Color(0xFF47626A),
    primary = Brand,
    onPrimary = Color.White,
    accentText = Color(0xFF155A6D),
    accentSoft = Color(0xFFD3ECF3),
    onAccentSoft = Color(0xFF073B49),
    sun = BrandSun,
    sunSoft = Color(0xFFFFF1C7),
    onSunSoft = Color(0xFF6A4A00),
    neutralSoft = Color(0xFFE9F0F2),
    heroCard = Color(0xFFD3ECF3),
    onHeroCard = Color(0xFF073B49),
    heroDevice = Color.White,
    success = Color(0xFF1E9B5C),
    successText = Color(0xFF17663F),
    successSoft = Color(0xFFE3F3EA),
    offline = Color(0xFF7A8F95),
    warning = Color(0xFF8A4500),
    warningText = Color(0xFF6B3600),
    warningSoft = Color(0xFFFFE9D1),
    danger = Color(0xFF9A2E24),
    disabled = Color(0xFFDCE7EA),
    disabledText = Color(0xFF47626A),
)

/** Dark appearance: Android's own neutral dark grays (owner's request), lagoon only for accents. */
val DarkColors = RohyColors(
    ground = Color(0xFF191C1D),
    surface = Color(0xFF262A2C),
    line = Color(0xFF353A3C),
    divider = Color(0xFF353A3C),
    controlLine = Color(0xFF454B4E),
    ink = Color(0xFFE1E3E3),
    muted = Color(0xFFBFC8CA),
    primary = Color(0xFF7FD0E6),
    onPrimary = Color(0xFF003543),
    accentText = Color(0xFFA6E0EF),
    accentSoft = Color(0xFF1F4B57),
    onAccentSoft = Color(0xFFCDEFF8),
    sun = Color(0xFFFFD25E),
    sunSoft = Color(0xFF4F3D0A),
    onSunSoft = Color(0xFFFFEDB8),
    neutralSoft = Color(0xFF2F3436),
    heroCard = Color(0xFF262A2C),
    onHeroCard = Color(0xFFE1E3E3),
    heroDevice = Color(0xFF353B3E),
    success = Color(0xFF6FDBA0),
    successText = Color(0xFF9BE8BD),
    successSoft = Color(0xFF233A2C),
    offline = Color(0xFF899295),
    warning = Color(0xFFF5B46E),
    warningText = Color(0xFFF5B46E),
    warningSoft = Color(0xFF43301A),
    danger = Color(0xFFFF9A8E),
    disabled = Color(0xFF2F3436),
    disabledText = Color(0xFFBFC8CA),
)

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

val LocalRohyColors = staticCompositionLocalOf { LightColors }

/** Rohy colors of the current appearance. */
val rohyColors: RohyColors @Composable get() = LocalRohyColors.current

/** Follows the system light or dark mode. */
@Composable
fun RohyTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = colors.primary,
        onPrimary = colors.onPrimary,
        primaryContainer = colors.accentSoft,
        onPrimaryContainer = colors.onAccentSoft,
        background = colors.ground,
        onBackground = colors.ink,
        surface = colors.surface,
        onSurface = colors.ink,
        surfaceContainerHigh = colors.surface,
        onSurfaceVariant = colors.muted,
        outline = colors.controlLine,
        error = colors.danger,
    )
    val typography = Typography().run {
        copy(
            headlineSmall = headlineSmall.copy(fontFamily = Inter),
            titleLarge = titleLarge.copy(fontFamily = Inter),
            titleMedium = titleMedium.copy(fontFamily = Inter),
            bodyLarge = bodyLarge.copy(fontFamily = Inter),
            bodyMedium = bodyMedium.copy(fontFamily = Inter),
            bodySmall = bodySmall.copy(fontFamily = Inter),
            labelLarge = labelLarge.copy(fontFamily = Inter),
        )
    }
    CompositionLocalProvider(LocalRohyColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}

/** Text styles of the board (Inter). */
object RohyText {
    val appTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.4).sp)
    val screenTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    val hero = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp)
    val bannerTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp)
    val cardTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp)
    val rowTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 21.sp)
    val body = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 22.sp)
    val small = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 19.sp)
    val smallStrong = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 19.sp)
    val section = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    val label = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.3.sp)
    val button = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
    val chip = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
}
