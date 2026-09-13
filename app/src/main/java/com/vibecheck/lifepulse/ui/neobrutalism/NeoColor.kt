package com.vibecheck.lifepulse.ui.neobrutalism

import androidx.compose.ui.graphics.Color

/**
 * Neobrutalism color palette: stark industrial neutrals, pure black structure, and a
 * small set of high-impact accents. Existing color names are kept stable so the rest
 * of the UI can use the stronger palette without API churn.
 */
object NeoColors {
    // Core neutrals
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
    val Cream = Color(0xFFF2F0E8)
    val OffWhiteSurface = Color(0xFFFFF8E7)
    val Ink = Color(0xFF171717)
    val Graphite = Color(0xFF2A2925)
    val CharcoalBlue = Color(0xFF1A2433)
    val Concrete = Color(0xFFE4E0D2)

    // High-impact accents
    val Lime = Color(0xFFB9FF00)
    val Yellow = Color(0xFFFFD400)
    val Orange = Color(0xFFFF4D00)
    val Purple = Color(0xFF315BFF)
    val Cyan = Color(0xFF00D1FF)
    val Pink = Color(0xFFFF2D20)
    val Red = Color(0xFFE11900)

    // Extended palette
    // Blues / cyans
    val PaleCyan = Color(0xFFDAF5F0)
    val AquaMist = Color(0xFFA7DBD8)
    val SkyBlue = Color(0xFF87CEEB)
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
    val Coral = Color(0xFFFF7A5C)
    val Salmon = Color(0xFFFF6B6B)

    // Pinks
    val BlushPink = Color(0xFFFCDFFF)
    val PastelPink = Color(0xFFFFC0CB)
    val BubblegumPink = Color(0xFFFFB2EF)
    val HotPink = Color(0xFFFF69B4)

    // Purples
    val Lavender = Color(0xFFE3DFF2)
    val LightViolet = Color(0xFFC4A1FF)
    val Amethyst = Color(0xFFA388EE)
    val DeepPurple = Color(0xFF9723C9)

    // Semantic roles
    val Background = Cream
    val Surface = OffWhiteSurface
    val Primary = Lime
    val Secondary = Cyan
    val Accent = Yellow
    val Danger = Red
    val OnPrimary = Ink
    val OnSurface = Ink
    val Border = Ink
    val Shadow = Graphite
    val Muted = Concrete
}

