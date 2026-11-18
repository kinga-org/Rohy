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

/** Colors of the design board "Rohy Android — refonte", one set per appearance. */
data class RohyColors(
    val ground: Color,
    val surface: Color,
    val line: Color,
    val divider: Color,
    val controlLine: Color,
    val ink: Color,
    val muted: Color,
    val logo: Color,
    val primary: Color,
    val accentText: Color,
    val accentSoft: Color,
    val neutralSoft: Color,
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

/** Brand teal: the connection banner keeps it in both appearances. */
val Brand = Color(0xFF1B4B5A)
val OnBrandLabel = Color(0xFFCFE3E8)
val OnBrandBody = Color(0xFFE3EEF1)

val LightColors = RohyColors(
    ground = Color(0xFFF4F7F7),
    surface = Color.White,
    line = Color(0xFFDCE4E6),
    divider = Color(0xFFE6ECEE),
    controlLine = Color(0xFFC5D2D5),
    ink = Color(0xFF12262C),
    muted = Color(0xFF4F6369),
    logo = Brand,
    primary = Brand,
    accentText = Brand,
    accentSoft = Color(0xFFE8F0F2),
    neutralSoft = Color(0xFFEEF1F2),
    success = Color(0xFF1E8A55),
    successText = Color(0xFF1E6B45),
    successSoft = Color(0xFFE3F3EA),
    offline = Color(0xFF7A8C91),
    warning = Color(0xFF9A4D00),
    warningText = Color(0xFF6B3600),
    warningSoft = Color(0xFFFFF1E0),
    danger = Color(0xFFA1352B),
    disabled = Color(0xFFD5E0E3),
    disabledText = Color(0xFF4F6369),
)

val DarkColors = RohyColors(
    ground = Color(0xFF0F1A1D),
    surface = Color(0xFF172429),
    line = Color(0xFF26363B),
    divider = Color(0xFF223136),
    controlLine = Color(0xFF34474D),
    ink = Color(0xFFE6EEF0),
    muted = Color(0xFF9AAEB3),
    logo = Color(0xFF2A6B7E),
    primary = Color(0xFF2A6B7E),
    accentText = Color(0xFF8CC7D6),
    accentSoft = Color(0xFF1E3A42),
    neutralSoft = Color(0xFF213035),
    success = Color(0xFF3CB97A),
    successText = Color(0xFF6FD39F),
    successSoft = Color(0xFF1C3A2B),
    offline = Color(0xFF6F858B),
    warning = Color(0xFFF2B36B),
    warningText = Color(0xFFF2B36B),
    warningSoft = Color(0xFF3A2A17),
    danger = Color(0xFFF08A7E),
    disabled = Color(0xFF213035),
    disabledText = Color(0xFF9AAEB3),
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
        onPrimary = Color.White,
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
    val appTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    val screenTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    val hero = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp)
    val bannerTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp)
    val cardTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp)
    val rowTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp)
    val body = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 22.sp)
    val small = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 19.sp)
    val smallStrong = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 19.sp)
    val section = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.3.sp)
    val label = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.3.sp)
    val button = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    val chip = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
}
