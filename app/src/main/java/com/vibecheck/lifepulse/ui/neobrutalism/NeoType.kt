package com.vibecheck.lifepulse.ui.neobrutalism

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vibecheck.lifepulse.R

val NeoDisplayFont = FontFamily(Font(R.font.neo_bungee))
val NeoNumberFont = FontFamily(Font(R.font.dashboard_space_grotesk_bold, FontWeight.Bold))
private val NeoDisplay = NeoDisplayFont
private fun display(size: Int, height: Int) = TextStyle(fontFamily = NeoDisplay, fontWeight = FontWeight.Bold,
    fontSize = size.sp, lineHeight = height.sp, fontFeatureSettings = "tnum")
private fun text(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.Default, fontWeight = weight, fontSize = size.sp, lineHeight = height.sp, fontFeatureSettings = "tnum")

/** Complete role mapping shared by every native screen and Material control. */
val NeoTypography = Typography(
    displayLarge = display(32, 38), displayMedium = display(28, 34), displaySmall = display(24, 30),
    headlineLarge = text(28, 34, FontWeight.Bold).copy(fontFamily = NeoNumberFont),
    headlineMedium = display(23, 29), headlineSmall = display(20, 26),
    titleLarge = display(16, 22), titleMedium = text(15, 20, FontWeight.Bold), titleSmall = text(14, 19, FontWeight.Bold),
    bodyLarge = text(15, 21), bodyMedium = text(14, 20), bodySmall = text(12, 17),
    labelLarge = text(13, 18, FontWeight.Bold), labelMedium = text(12, 17, FontWeight.Bold), labelSmall = text(11, 15, FontWeight.Bold)
)
