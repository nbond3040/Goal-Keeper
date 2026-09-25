package com.goalkeeper.core.model

/**
 * How much a goal matters. Drives list ranking (higher first) and how many reminder
 * nudges a goal gets by default once its daily reminder window opens.
 */
enum class Importance(val level: Int, val label: String, val defaultNudges: Int) {
    LOW(1, "Low", 1),
    MEDIUM(2, "Medium", 2),
    HIGH(3, "High", 3),
    CRITICAL(4, "Critical", 4);

    companion object {
        fun fromLevel(level: Int): Importance = entries.firstOrNull { it.level == level } ?: MEDIUM

        /** Highest importance first — the order sections appear in ranked lists. */
        val descending: List<Importance> = entries.sortedByDescending { it.level }
    }
}
