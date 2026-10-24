package com.vibecheck.lifepulse.ui.neobrutalism

import androidx.compose.ui.graphics.Color

/**
 * Comic pop palette: warm paper, black print, and saturated domain surfaces.
 * Existing role names stay stable for every screen and reusable control.
 */
object NeoColors {
    // Core neutrals
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
    val Cream = Color(0xFFFFF6E7)
    val OffWhiteSurface = Color(0xFFFFFDF5)
    val Ink = Color(0xFF171717)
    val Graphite = Ink
    val CharcoalBlue = Color(0xFF1A2433)
    val Concrete = Color(0xFFE4E0D2)

    // High-impact accents
    val Lime = Color(0xFFC4F86A)
    val Yellow = Color(0xFFFFDF32)
    val Orange = Color(0xFFFF4D00)
    val Purple = Color(0xFF315BFF)
    val Cyan = Color(0xFF00D1FF)
    val Pink = Color(0xFFFF2D20)
    val Red = Color(0xFFE11900)

    // Extended palette
    // Blues / cyans
    val PaleCyan = Color(0xFFDAF5F0)
    val AquaMist = Color(0xFFA7DBD8)
    val SkyBlue = Color(0xFF64CDFF)
    val TealBlue = Color(0xFF69D2E7)

    // Greens
    val SageGreen = Color(0xFFB5D2AD)
    val MintGreen = Color(0xFFBAFCA2)
    val LightGreen = Color(0xFF90EE90)
    val OliveGreen = Color(0xFF7FBC8C)

    // Yellows
    val PastelYellow = Color(0xFFFDFD96)
    val Mustard = Color(0xFFFFDB58)
    val GoldenYellow = Color(0xFFF4D738)
    val Amber = Color(0xFFE3A018)

    // Oranges / corals
    val PeachTan = Color(0xFFF8D6B3)
    val LightSalmon = Color(0xFFFFA07A)
    val Coral = Color(0xFFFF896A)
    val Salmon = Color(0xFFFF6B6B)

    // Pinks
    val BlushPink = Color(0xFFFCDFFF)
    val PastelPink = Color(0xFFFFC0CB)
    val BubblegumPink = Color(0xFFFFB2EF)
    val HotPink = Color(0xFFFF69B4)

    // Purples
    val Lavender = Color(0xFFE3DFF2)
    val LightViolet = Color(0xFFC4A4FF)
    val Amethyst = Color(0xFFA388EE)
    val DeepPurple = Color(0xFF9723C9)

    // Semantic roles
    val Background = Cream
    val Surface = OffWhiteSurface
    val Primary = Yellow
    val Secondary = SkyBlue
    val Accent = Yellow
    val Danger = Red
    val OnPrimary = Ink
    val OnSurface = Ink
    val Border = Ink
    val Shadow = Graphite
    val Muted = Concrete
    val Habit = SkyBlue
    val Expense = Coral
    val Success = Lime
    val SecondaryText = Color(0xFF403A33)
    val Rule = Color(0xFFC8C3B5)
}

