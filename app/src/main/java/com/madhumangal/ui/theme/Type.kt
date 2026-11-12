package com.madhumangal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

fun getDynamicTypography(fontScale: Float = 1.0f, fontMode: String = "DEFAULT"): Typography {
    val scale = fontScale.coerceIn(0.75f, 1.4f)
    val weightBold = when (fontMode) {
        "THIN" -> FontWeight.Medium
        "BOLD" -> FontWeight.Black
        else -> FontWeight.Bold
    }
    val weightSemi = when (fontMode) {
        "THIN" -> FontWeight.Normal
        "BOLD" -> FontWeight.Bold
        else -> FontWeight.SemiBold
    }
    val weightRegular = when (fontMode) {
        "THIN" -> FontWeight.Light
        "BOLD" -> FontWeight.Medium
        else -> FontWeight.Normal
    }

    return Typography(
        headlineLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = weightBold,
            fontSize = (28 * scale).sp,
            color = TextPrimary
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = weightSemi,
            fontSize = (20 * scale).sp,
            color = TextPrimary
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = weightSemi,
            fontSize = (16 * scale).sp,
            color = TextPrimary
        ),
        titleSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = weightSemi,
            fontSize = (14 * scale).sp,
            color = TextPrimary
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = weightRegular,
            fontSize = (16 * scale).sp,
            color = TextPrimary
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = weightRegular,
            fontSize = (14 * scale).sp,
            color = TextSecondary
        ),
        bodySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = weightRegular,
            fontSize = (12 * scale).sp,
            color = TextSecondary
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = weightRegular,
            fontSize = (11 * scale).sp,
            color = TextMuted
        )
    )
}

val Typography = getDynamicTypography()
