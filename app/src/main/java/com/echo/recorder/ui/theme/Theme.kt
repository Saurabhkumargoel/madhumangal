package com.echo.recorder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import org.json.JSONObject

data class AppThemeOption(
    val id: String,
    val name: String,
    val description: String,
    val primaryColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val accentColor: Color
)

object AppThemeColors {
    var darkBackground by mutableStateOf(Color(0xFF0F172A))
    var surfaceDark by mutableStateOf(Color(0xFF1E293B))
    var surfaceVariantDark by mutableStateOf(Color(0xFF334155))
    var primaryBlue by mutableStateOf(Color(0xFF38BDF8))
    var accentPurple by mutableStateOf(Color(0xFFA855F7))
    var activeRed by mutableStateOf(Color(0xFFEF4444))
    var textPrimary by mutableStateOf(Color(0xFFF8FAFC))
    var textSecondary by mutableStateOf(Color(0xFF94A3B8))
    var textMuted by mutableStateOf(Color(0xFF64748B))
    var cardBorder by mutableStateOf(Color(0xFF334155))

    fun updateFromScheme(scheme: ColorScheme, themeId: String, customBorderHex: String? = null) {
        darkBackground = scheme.background
        surfaceDark = scheme.surface
        surfaceVariantDark = scheme.surfaceVariant
        primaryBlue = scheme.primary
        accentPurple = scheme.secondary
        activeRed = scheme.tertiary
        textPrimary = scheme.onBackground
        textSecondary = scheme.onSurface.copy(alpha = 0.75f)
        textMuted = scheme.onSurface.copy(alpha = 0.5f)
        cardBorder = when {
            customBorderHex != null -> try { Color(android.graphics.Color.parseColor(customBorderHex)) } catch (_: Exception) { Color(0xFF334155) }
            themeId == "AMOLED_HIGH" -> Color(0xFF161616)
            themeId == "AMOLED_BLACK" -> Color(0xFF222222)
            themeId == "LIGHT" -> Color(0xFFE2E8F0)
            themeId == "CYBERPUNK" -> Color(0xFF4A2574)
            themeId == "DRACULA" -> Color(0xFF6272A4)
            themeId == "EMERALD_FOREST" -> Color(0xFF1B4D3E)
            else -> Color(0xFF334155)
        }
    }
}

object AppThemes {
    // 1. AMOLED High: 100% Pure Pitch Black everywhere, minimal shades of gray
    val AmoledHigh = darkColorScheme(
        primary = Color(0xFF38BDF8),
        secondary = Color(0xFFA855F7),
        tertiary = Color(0xFFEF4444),
        background = Color(0xFF000000),    // Pure Black
        surface = Color(0xFF000000),       // Pure Black Surface
        surfaceVariant = Color(0xFF0D0D0D), // Ultra deep gray
        onPrimary = Color(0xFF000000),
        onSecondary = Color(0xFF000000),
        onBackground = Color(0xFFF9FAFB),
        onSurface = Color(0xFFF9FAFB)
    )

    // 2. Light Theme: Crisp, corporate light styling
    val LightMode = lightColorScheme(
        primary = Color(0xFF0284C7),
        secondary = Color(0xFF7C3AED),
        tertiary = Color(0xFFDC2626),
        background = Color(0xFFF8FAFC),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF1F5F9),
        onPrimary = Color(0xFFFFFFFF),
        onSecondary = Color(0xFFFFFFFF),
        onBackground = Color(0xFF0F172A),
        onSurface = Color(0xFF0F172A)
    )

    // 3. Dark Theme: Clean standard dark slate
    val DarkSlate = darkColorScheme(
        primary = Color(0xFF38BDF8),
        secondary = Color(0xFFA855F7),
        tertiary = Color(0xFFEF4444),
        background = Color(0xFF0B0F17),
        surface = Color(0xFF151D2A),
        surfaceVariant = Color(0xFF232F42),
        onPrimary = Color(0xFF0F172A),
        onSecondary = Color(0xFFF8FAFC),
        onBackground = Color(0xFFF8FAFC),
        onSurface = Color(0xFFF8FAFC)
    )

    val MidnightBlue = darkColorScheme(
        primary = Color(0xFF38BDF8),
        secondary = Color(0xFFA855F7),
        tertiary = Color(0xFFEF4444),
        background = Color(0xFF0F172A),
        surface = Color(0xFF1E293B),
        surfaceVariant = Color(0xFF334155),
        onPrimary = Color(0xFF0F172A),
        onSecondary = Color(0xFFF8FAFC),
        onBackground = Color(0xFFF8FAFC),
        onSurface = Color(0xFFF8FAFC)
    )

    val Cyberpunk = darkColorScheme(
        primary = Color(0xFFFFE600),
        secondary = Color(0xFF00F0FF),
        tertiary = Color(0xFFFF0055),
        background = Color(0xFF0D0221),
        surface = Color(0xFF1B1035),
        surfaceVariant = Color(0xFF3D1E6D),
        onPrimary = Color(0xFF0D0221),
        onSecondary = Color(0xFF0D0221),
        onBackground = Color(0xFFFDFEFE),
        onSurface = Color(0xFFFDFEFE)
    )

    val Dracula = darkColorScheme(
        primary = Color(0xFFBD93F9),
        secondary = Color(0xFF50FA7B),
        tertiary = Color(0xFFFF79C6),
        background = Color(0xFF282A36),
        surface = Color(0xFF44475A),
        surfaceVariant = Color(0xFF6272A4),
        onPrimary = Color(0xFF282A36),
        onSecondary = Color(0xFF282A36),
        onBackground = Color(0xFFF8F8F2),
        onSurface = Color(0xFFF8F8F2)
    )

    val EmeraldForest = darkColorScheme(
        primary = Color(0xFF10B981),
        secondary = Color(0xFF34D399),
        tertiary = Color(0xFFF59E0B),
        background = Color(0xFF05201A),
        surface = Color(0xFF0A362C),
        surfaceVariant = Color(0xFF134E42),
        onPrimary = Color(0xFF05201A),
        onSecondary = Color(0xFF05201A),
        onBackground = Color(0xFFECFDF5),
        onSurface = Color(0xFFECFDF5)
    )

    val AmoledBlack = darkColorScheme(
        primary = Color(0xFF38BDF8),
        secondary = Color(0xFFA855F7),
        tertiary = Color(0xFFEF4444),
        background = Color(0xFF000000),
        surface = Color(0xFF121212),
        surfaceVariant = Color(0xFF242424),
        onPrimary = Color(0xFF000000),
        onSecondary = Color(0xFF000000),
        onBackground = Color(0xFFFFFFFF),
        onSurface = Color(0xFFFFFFFF)
    )

    val TelegramTheme = darkColorScheme(
        primary = Color(0xFF5288C1),
        secondary = Color(0xFF64B5F6),
        tertiary = Color(0xFF2B5278),
        background = Color(0xFF0E1621),
        surface = Color(0xFF17212B),
        surfaceVariant = Color(0xFF242F3D),
        onPrimary = Color(0xFFFFFFFF),
        onSecondary = Color(0xFFFFFFFF),
        onBackground = Color(0xFFF5F5F5),
        onSurface = Color(0xFFF5F5F5)
    )

    val GradientBlack = darkColorScheme(
        primary = Color(0xFF38BDF8),
        secondary = Color(0xFF94A3B8),
        tertiary = Color(0xFF10B981),
        background = Color(0xFF07090E),
        surface = Color(0xFF10141D),
        surfaceVariant = Color(0xFF181F2B),
        onPrimary = Color(0xFF07090E),
        onSecondary = Color(0xFFF8FAFC),
        onBackground = Color(0xFFF8FAFC),
        onSurface = Color(0xFFF8FAFC)
    )

    val ALL_THEMES = listOf(
        AppThemeOption(
            id = "GRADIENT_BLACK",
            name = "Obsidian Gradient Black",
            description = "Silky gradient black tones, zero neon, high-contrast slate",
            primaryColor = Color(0xFF38BDF8),
            backgroundColor = Color(0xFF07090E),
            surfaceColor = Color(0xFF10141D),
            accentColor = Color(0xFF94A3B8)
        ),
        AppThemeOption(
            id = "LIGHT",
            name = "Light Mode",
            description = "Crisp corporate light palette with high legibility",
            primaryColor = Color(0xFF0284C7),
            backgroundColor = Color(0xFFF8FAFC),
            surfaceColor = Color(0xFFFFFFFF),
            accentColor = Color(0xFF7C3AED)
        ),
        AppThemeOption(
            id = "DARK",
            name = "Dark Mode",
            description = "Clean deep dark slate for high contrast",
            primaryColor = Color(0xFF38BDF8),
            backgroundColor = Color(0xFF0B0F17),
            surfaceColor = Color(0xFF151D2A),
            accentColor = Color(0xFFA855F7)
        ),
        AppThemeOption(
            id = "SYSTEM",
            name = "System Default",
            description = "Adapts automatically to Android system dark/light theme",
            primaryColor = Color(0xFF38BDF8),
            backgroundColor = Color(0xFF0F172A),
            surfaceColor = Color(0xFF1E293B),
            accentColor = Color(0xFFA855F7)
        ),
        AppThemeOption(
            id = "AMOLED_HIGH",
            name = "AMOLED High",
            description = "True pitch black pixels, zero gray haze, ultra-minimal styling",
            primaryColor = Color(0xFF38BDF8),
            backgroundColor = Color(0xFF000000),
            surfaceColor = Color(0xFF000000),
            accentColor = Color(0xFFA855F7)
        ),
        AppThemeOption(
            id = "AMOLED_BLACK",
            name = "AMOLED Pitch",
            description = "Pitch black background with subtle dark gray accents",
            primaryColor = Color(0xFF38BDF8),
            backgroundColor = Color(0xFF000000),
            surfaceColor = Color(0xFF121212),
            accentColor = Color(0xFFA855F7)
        ),
        AppThemeOption(
            id = "TELEGRAM",
            name = "Telegram Dark",
            description = "Authentic Telegram slate blue & dark chat surface aesthetic",
            primaryColor = Color(0xFF5288C1),
            backgroundColor = Color(0xFF0E1621),
            surfaceColor = Color(0xFF17212B),
            accentColor = Color(0xFF2B5278)
        ),
        AppThemeOption(
            id = "MIDNIGHT_BLUE",
            name = "Midnight Blue",
            description = "Modern dark slate & sky blue",
            primaryColor = Color(0xFF38BDF8),
            backgroundColor = Color(0xFF0F172A),
            surfaceColor = Color(0xFF1E293B),
            accentColor = Color(0xFFA855F7)
        ),
        AppThemeOption(
            id = "DRACULA",
            name = "Dracula",
            description = "Developer dark theme with pastel purple & green",
            primaryColor = Color(0xFFBD93F9),
            backgroundColor = Color(0xFF282A36),
            surfaceColor = Color(0xFF44475A),
            accentColor = Color(0xFF50FA7B)
        ),
        AppThemeOption(
            id = "EMERALD_FOREST",
            name = "Emerald Forest",
            description = "Calming deep pine green & mint emerald",
            primaryColor = Color(0xFF10B981),
            backgroundColor = Color(0xFF05201A),
            surfaceColor = Color(0xFF0A362C),
            accentColor = Color(0xFF34D399)
        ),
        AppThemeOption(
            id = "CYBERPUNK",
            name = "Cyberpunk Neon",
            description = "Electric neon yellow, cyan & magenta",
            primaryColor = Color(0xFFFFE600),
            backgroundColor = Color(0xFF0D0221),
            surfaceColor = Color(0xFF1B1035),
            accentColor = Color(0xFF00F0FF)
        ),
        AppThemeOption(
            id = "CUSTOM",
            name = "Custom Theme (Clone & Create)",
            description = "Personalized custom palette cloned from any preset or created from scratch",
            primaryColor = Color(0xFF38BDF8),
            backgroundColor = Color(0xFF0F172A),
            surfaceColor = Color(0xFF1E293B),
            accentColor = Color(0xFFA855F7)
        )
    )

    fun getColorScheme(themeId: String, isSystemDark: Boolean, customJson: String): Pair<ColorScheme, String?> {
        if (themeId == "CUSTOM" && customJson.isNotBlank()) {
            try {
                val json = JSONObject(customJson)
                val primary = Color(android.graphics.Color.parseColor(json.optString("primary", "#38BDF8")))
                val bg = Color(android.graphics.Color.parseColor(json.optString("background", "#0F172A")))
                val surface = Color(android.graphics.Color.parseColor(json.optString("surface", "#1E293B")))
                val border = json.optString("border", "#334155")
                val isDark = bg.red + bg.green + bg.blue < 1.5f
                val scheme = if (isDark) {
                    darkColorScheme(
                        primary = primary,
                        secondary = primary,
                        background = bg,
                        surface = surface,
                        surfaceVariant = surface,
                        onPrimary = if (isDark) Color.Black else Color.White,
                        onBackground = if (isDark) Color.White else Color.Black,
                        onSurface = if (isDark) Color.White else Color.Black
                    )
                } else {
                    lightColorScheme(
                        primary = primary,
                        secondary = primary,
                        background = bg,
                        surface = surface,
                        surfaceVariant = surface,
                        onPrimary = Color.White,
                        onBackground = Color.Black,
                        onSurface = Color.Black
                    )
                }
                return Pair(scheme, border)
            } catch (_: Exception) {}
        }

        val scheme = when (themeId) {
            "GRADIENT_BLACK" -> GradientBlack
            "AMOLED_HIGH" -> AmoledHigh
            "LIGHT" -> LightMode
            "DARK" -> DarkSlate
            "TELEGRAM" -> TelegramTheme
            "SYSTEM" -> if (isSystemDark) DarkSlate else LightMode
            "CYBERPUNK" -> Cyberpunk
            "DRACULA" -> Dracula
            "EMERALD_FOREST" -> EmeraldForest
            "AMOLED_BLACK" -> AmoledBlack
            else -> MidnightBlue
        }
        return Pair(scheme, null)
    }
}

@Composable
fun MadhumangalTheme(
    themeName: String = "MIDNIGHT_BLUE",
    customThemeJson: String = "",
    fontScale: Float = 1.0f,
    fontMode: String = "DEFAULT",
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val (colorScheme, customBorder) = AppThemes.getColorScheme(themeName, isSystemDark, customThemeJson)

    // Synchronously update dynamic theme state
    AppThemeColors.updateFromScheme(colorScheme, themeName, customBorder)

    val dynamicTypography = getDynamicTypography(fontScale, fontMode)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = dynamicTypography,
        content = content
    )
}

@Composable
fun GoodEchoTheme(
    themeName: String = "MIDNIGHT_BLUE",
    customThemeJson: String = "",
    fontScale: Float = 1.0f,
    fontMode: String = "DEFAULT",
    content: @Composable () -> Unit
) {
    MadhumangalTheme(
        themeName = themeName,
        customThemeJson = customThemeJson,
        fontScale = fontScale,
        fontMode = fontMode,
        content = content
    )
}
