package com.vibecheck.lifepulse.ui.neobrutalism

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Custom Shapes for the Neobrutalism style: mostly slightly rounded corners,
 * with a fully pill-shaped option for chips/tags.
 */
data class NeoShapes(
    val small: RoundedCornerShape = RoundedCornerShape(8.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(12.dp),
    val large: RoundedCornerShape = RoundedCornerShape(16.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(50)
)

val LocalNeoShapesDefault = NeoShapes()

