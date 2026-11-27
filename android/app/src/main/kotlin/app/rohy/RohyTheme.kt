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

/** Colors of the design board "Rohy Android — refonte" (palette Myrtille & Pêche), one set per appearance. */
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
    /** Soft blueberry: icon tiles, the connection card. */
    val accentSoft: Color,
    val onAccentSoft: Color,
    /** Peach, for small cheerful details (heart, cheeks, some icon tiles). */
    val peach: Color,
    val peachSoft: Color,
    val onPeachSoft: Color,
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

/** Blueberry of the app icon: the same in both appearances. */
val Brand = Color(0xFF5653D0)

/** Peach of the clipboard clip in the app icon. */
val BrandPeach = Color(0xFFFF9F6E)

val LightColors = RohyColors(
    ground = Color(0xFFF8F7FD),
    surface = Color.White,
    line = Color(0xFFE9E8F4),
    divider = Color(0xFFEDECF6),
    controlLine = Color(0xFFD3D1EC),
    ink = Color(0xFF1D1B39),
    muted = Color(0xFF57557A),
    primary = Brand,
    onPrimary = Color.White,
    accentText = Color(0xFF4643B8),
    accentSoft = Color(0xFFE5E4FB),
    onAccentSoft = Color(0xFF24206B),
    peach = BrandPeach,
    peachSoft = Color(0xFFFFE8DA),
    onPeachSoft = Color(0xFF7A3410),
    neutralSoft = Color(0xFFEEEDF6),
    heroCard = Color(0xFFE5E4FB),
    onHeroCard = Color(0xFF24206B),
    heroDevice = Color.White,
    success = Color(0xFF1E9B5C),
    successText = Color(0xFF17663F),
    successSoft = Color(0xFFE3F3EA),
    offline = Color(0xFF7D7B9C),
    warning = Color(0xFF8A4500),
    warningText = Color(0xFF6B3600),
    warningSoft = Color(0xFFFFE9D1),
    danger = Color(0xFF9A2E24),
    disabled = Color(0xFFE2E1EE),
    disabledText = Color(0xFF57557A),
)

/** Dark appearance: Android's own neutral dark grays (owner's request), blueberry only for accents. */
val DarkColors = RohyColors(
    ground = Color(0xFF1C1B1F),
    surface = Color(0xFF2B2930),
    line = Color(0xFF3A3840),
    divider = Color(0xFF3A3840),
    controlLine = Color(0xFF4A4850),
    ink = Color(0xFFE6E1E5),
    muted = Color(0xFFCAC4D0),
    primary = Color(0xFFBDBBFF),
    onPrimary = Color(0xFF23206A),
    accentText = Color(0xFFCFCDFF),
    accentSoft = Color(0xFF3B3870),
    onAccentSoft = Color(0xFFE4E3FF),
    peach = Color(0xFFFFB794),
    peachSoft = Color(0xFF5A3424),
    onPeachSoft = Color(0xFFFFDCCB),
    neutralSoft = Color(0xFF36343B),
    heroCard = Color(0xFF2B2930),
    onHeroCard = Color(0xFFE6E1E5),
    heroDevice = Color(0xFF3E3C45),
    success = Color(0xFF6FDBA0),
    successText = Color(0xFF9BE8BD),
    successSoft = Color(0xFF233A2C),
    offline = Color(0xFF938F99),
    warning = Color(0xFFF5B46E),
    warningText = Color(0xFFF5B46E),
    warningSoft = Color(0xFF43301A),
    danger = Color(0xFFFF9A8E),
    disabled = Color(0xFF36343B),
    disabledText = Color(0xFFCAC4D0),
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
