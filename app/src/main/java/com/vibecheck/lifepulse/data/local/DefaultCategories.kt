package com.vibecheck.lifepulse.data.local

import android.content.Context
import com.vibecheck.lifepulse.R

/** Single source of truth for the built-in expense categories shipped with the app. */
object DefaultCategories {
    fun all(context: Context): List<Pair<String, String>> = listOf(
        R.string.default_category_food to "#FF7043",
        R.string.default_category_transport to "#42A5F5",
        R.string.default_category_groceries to "#66BB6A",
        R.string.default_category_bills to "#AB47BC",
        R.string.default_category_health to "#EF5350",
        R.string.default_category_entertainment to "#FFCA28",
        R.string.default_category_other to "#78909C"
    ).map { (nameRes, color) -> context.getString(nameRes) to color }
}

