package com.vibecheck.lifepulse.data.local

/** Single source of truth for the built-in expense categories shipped with the app. */
object DefaultCategories {
    val ALL: List<Pair<String, String>> = listOf(
        "Food" to "#FF7043",
        "Transport" to "#42A5F5",
        "Groceries" to "#66BB6A",
        "Bills" to "#AB47BC",
        "Health" to "#EF5350",
        "Entertainment" to "#FFCA28",
        "Other" to "#78909C"
    )
}

