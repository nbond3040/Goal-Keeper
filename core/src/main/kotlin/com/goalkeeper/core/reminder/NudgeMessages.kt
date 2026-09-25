package com.goalkeeper.core.reminder

/** Title and body of a notification. */
data class NudgeText(val title: String, val body: String)

/** Motivational notification copy. Deterministic: the same inputs always give the same text. */
object NudgeMessages {

    /**
     * Copy for nudge [index] (0-based) of [count] for one goal.
     *
     * Tone escalates: the first nudge is friendly, middle ones firmer, and the last one (when [count] > 1)
     * is a clear "last call" that the streak ends at midnight. Mentions the [streak] length when it is > 0,
     * otherwise encourages starting a new streak today. When [why] is not blank, the body ends with it as a
     * reminder of the user's reason (e.g. "Why: …"). [variant] picks between alternative phrasings
     * (callers pass something like the day of year) so messages don't feel repetitive.
     */
    fun nudge(goalTitle: String, why: String, streak: Int, index: Int, count: Int, variant: Int = 0): NudgeText {
        val tone = toneFor(index, count)
        val variants = bodyVariants(tone, goalTitle, streak)
        val body = variants[Math.floorMod(variant, variants.size)]
        val fullBody = if (why.isNotBlank()) "$body\nWhy: ${why.trim()}" else body
        return NudgeText(title = title(goalTitle, streak, tone), body = fullBody)
    }

    /**
     * Morning briefing: how many goals are scheduled today, the top-ranked one, and the longest active streak.
     * [scheduledToday] may be 0 (a rest day message).
     */
    fun briefing(scheduledToday: Int, topGoalTitle: String?, longestStreak: Int): NudgeText {
        val title = when {
            scheduledToday == 0 -> "Rest day"
            scheduledToday == 1 -> "1 goal today"
            else -> "$scheduledToday goals today"
        }

        val sentences = mutableListOf<String>()
        sentences += when {
            scheduledToday == 0 -> "Nothing is scheduled today — a good day to rest or get ahead."
            scheduledToday == 1 -> "You have 1 goal scheduled today."
            else -> "You have $scheduledToday goals scheduled today."
        }
        if (topGoalTitle != null) {
            sentences += "Top priority: $topGoalTitle."
        }
        if (longestStreak > 0) {
            sentences += "Longest active streak: ${streakPhrase(longestStreak)}."
        }

        return NudgeText(title = title, body = sentences.joinToString(" "))
    }

    /** Short celebration line when a check-in lands exactly on a streak milestone, e.g. "30-day streak on Run!". */
    fun milestone(goalTitle: String, streak: Int): String = "${streakPhrase(streak)} on $goalTitle! Keep going."

    private enum class Tone { FRIENDLY, FIRM, LAST_CALL }

    /** index 0 is friendly; the last index (only when there's more than one nudge) is the last call; between is firmer. */
    private fun toneFor(index: Int, count: Int): Tone = when {
        count <= 1 -> Tone.FRIENDLY
        index >= count - 1 -> Tone.LAST_CALL
        index <= 0 -> Tone.FRIENDLY
        else -> Tone.FIRM
    }

    private fun streakPhrase(streak: Int): String = if (streak == 1) "1-day streak" else "$streak-day streak"

    private fun title(goalTitle: String, streak: Int, tone: Tone): String {
        val suffix = if (streak > 0) {
            val phrase = streakPhrase(streak)
            if (tone == Tone.LAST_CALL) "$phrase ends tonight" else "$phrase at risk"
        } else {
            if (tone == Tone.LAST_CALL) "last call to start a streak" else "start a streak today"
        }
        return "$goalTitle · $suffix"
    }

    /** 2-4 deterministic phrasings for [tone], picked by [variant] via [Math.floorMod] in [nudge]. */
    private fun bodyVariants(tone: Tone, goalTitle: String, streak: Int): List<String> {
        val streakText = streakPhrase(streak)
        return when (tone) {
            Tone.FRIENDLY -> if (streak > 0) listOf(
                "You're on a $streakText for $goalTitle. A quick check-in keeps it going.",
                "Keep your $streakText on $goalTitle alive — it only takes a minute.",
                "Protect your $streakText on $goalTitle with today's check-in.",
            ) else listOf(
                "Start a streak on $goalTitle today — the first check-in is the hardest part.",
                "No streak yet on $goalTitle. Today's a good day to begin one.",
                "Kick off a new streak on $goalTitle with a check-in today.",
            )

            Tone.FIRM -> if (streak > 0) listOf(
                "Don't let your $streakText on $goalTitle slip — check in before you lose momentum.",
                "Still time to protect your $streakText on $goalTitle. Check in now.",
                "Your $streakText on $goalTitle needs today's check-in — don't break it now.",
            ) else listOf(
                "Still no check-in on $goalTitle today. Start your streak before the day gets away.",
                "$goalTitle is waiting — check in and start a new streak.",
                "Don't let today slip by without a check-in on $goalTitle.",
            )

            Tone.LAST_CALL -> if (streak > 0) listOf(
                "Last call: your $streakText on $goalTitle ends at midnight without a check-in.",
                "Final reminder — check in now or your $streakText on $goalTitle ends today at midnight.",
                "Midnight is close. Check in now to keep your $streakText on $goalTitle alive before the day ends.",
            ) else listOf(
                "Last call to start a streak on $goalTitle — the day ends at midnight.",
                "Final reminder: check in on $goalTitle before midnight to start your streak.",
                "Midnight is close — a check-in now starts your streak on $goalTitle before the day ends.",
            )
        }
    }
}
