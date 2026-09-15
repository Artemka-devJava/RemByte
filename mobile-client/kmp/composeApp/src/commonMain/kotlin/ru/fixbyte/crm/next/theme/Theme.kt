package ru.fixbyte.crm.next.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

// Тот же фирменный синий, что и в старых Android/iOS-клиентах (mobile-client/*/…/colors.xml).
private val BrandLight = Color(0xFF2563EB)
private val BrandDarkVariant = Color(0xFF1D4ED8)
private val BgLight = Color(0xFFF1F5F9)
private val TextLight = Color(0xFF1E293B)
private val MutedLight = Color(0xFF64748B)
private val DangerLight = Color(0xFFDC2626)
private val SurfaceAltLight = Color(0xFFE7EEFB)
private val DividerLight = Color(0xFFE2E8F0)

private val BrandDark = Color(0xFF3B82F6)
private val BgDark = Color(0xFF0F172A)
private val TextDark = Color(0xFFE2E8F0)
private val MutedDark = Color(0xFF94A3B8)
private val DangerDark = Color(0xFFF87171)
private val SurfaceAltDark = Color(0xFF1E293B)
private val DividerDark = Color(0xFF334155)

private val LightColors = lightColorScheme(
    primary = BrandLight,
    onPrimary = Color.White,
    primaryContainer = SurfaceAltLight,
    onPrimaryContainer = BrandDarkVariant,
    background = BgLight,
    onBackground = TextLight,
    surface = Color.White,
    onSurface = TextLight,
    surfaceVariant = SurfaceAltLight,
    onSurfaceVariant = MutedLight,
    outline = DividerLight,
    error = DangerLight,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = BrandDark,
    onPrimary = Color.White,
    primaryContainer = SurfaceAltDark,
    onPrimaryContainer = BrandDark,
    background = BgDark,
    onBackground = TextDark,
    surface = Color(0xFF162032),
    onSurface = TextDark,
    surfaceVariant = SurfaceAltDark,
    onSurfaceVariant = MutedDark,
    outline = DividerDark,
    error = DangerDark,
    onError = Color.White
)

private val AppTypography = Typography(
    titleLarge = Typography().titleLarge.copy(fontSize = 22.sp),
)

@Composable
fun FixByteTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}
