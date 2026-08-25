package com.brutal.accountability.data

import org.json.JSONObject
import java.util.Locale

internal object InterventionContent {

    fun personalizedFallbackLines(
        nickname: String,
        currentAppLabel: String,
        goal: String,
        insecurity: String = "",
        fear: String = "",
        profession: String = "",
        relationshipStatus: String = "",
        gymStatus: String = ""
    ): List<String> {
        val insecurityTail = if (insecurity.isBlank()) {
            ""
        } else {
            " Aur haan, $insecurity abhi bhi fix nahi hua."
        }
        val fearTail = if (fear.isBlank()) {
            ""
        } else {
            " Yehi pace raha toh $fear sach ho jayega."
        }
        val professionTail = if (profession.isBlank()) {
            ""
        } else {
            " $profession hoke bhi discipline zero."
        }
        val relationshipTail = if (relationshipStatus.isBlank()) {
            ""
        } else {
            " $relationshipStatus ho ya single, excuses sabko cheap lagte hain."
        }
        val gymTail = if (gymStatus.isBlank()) {
            ""
        } else {
            " Gym status '$gymStatus' bolne se body aur confidence nahi banega."
        }

        return listOf(
            "$nickname, $currentAppLabel phir se? $goal khud se complete nahi hoga.$fearTail",
            "$nickname, abhi $currentAppLabel band kar. Discipline ke bina $goal sirf fantasy hai.$insecurityTail",
            "$nickname, tu live mode me apna future trade kar raha hai for $currentAppLabel. Back to $goal.",
            "$nickname, har swipe tera future salary aur respect ka cut hai.$professionTail",
            "$nickname, focus tod ke tu apni image khud destroy kar raha hai.$relationshipTail",
            "$nickname, $goal ka sapna bolta hai aur action me zero deta hai.$gymTail"
        )
    }

    fun pickFreshLine(candidates: List<String>, recentLines: Collection<String>): String {
        val recent = recentLines.map { it.lowercase(Locale.getDefault()) }.toSet()
        val filtered = candidates.filterNot { it.lowercase(Locale.getDefault()) in recent }
        val pool = if (filtered.isEmpty()) candidates else filtered
        return pool.random()
    }

    fun parseLeverageJson(leverageJson: String): List<Pair<String, String>> {
        return try {
            val json = JSONObject(leverageJson)
            json.keys().asSequence().map { key ->
                key to json.optString(key)
            }.toList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
