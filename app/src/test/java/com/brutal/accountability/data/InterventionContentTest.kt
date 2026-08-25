package com.brutal.accountability.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InterventionContentTest {

    @Test
    fun `fallback lines include nickname and app label`() {
        val lines = InterventionContent.personalizedFallbackLines(
            nickname = "Sarthak",
            currentAppLabel = "Instagram",
            goal = "crack UPSC"
        )

        assertEquals(6, lines.size)
        lines.forEach { line ->
            assertTrue(line.contains("Sarthak"))
            assertTrue(line.isNotBlank())
        }
        val joined = lines.joinToString(" ")
        assertTrue(joined.contains("Instagram"))
        assertTrue(joined.contains("UPSC"))
    }

    @Test
    fun `fallback lines append personal tails only when provided`() {
        val withoutTails = InterventionContent.personalizedFallbackLines(
            nickname = "A",
            currentAppLabel = "X",
            goal = "G"
        ).joinToString(" ")

        val withTails = InterventionContent.personalizedFallbackLines(
            nickname = "A",
            currentAppLabel = "X",
            goal = "G",
            insecurity = "public speaking",
            fear = "failure",
            profession = "engineer",
            relationshipStatus = "single",
            gymStatus = "irregular"
        ).joinToString(" ")

        assertFalse(withoutTails.contains("public speaking"))
        assertFalse(withTails.contains("placeholder"))
        listOf("public speaking", "failure", "engineer", "single", "irregular").forEach { tail ->
            assertTrue("expected '$tail' in: $withTails", withTails.contains(tail))
        }
    }

    @Test
    fun `pickFreshLine avoids recent lines when alternatives exist`() {
        val candidates = listOf("alpha", "beta", "gamma")
        repeat(50) {
            val picked = InterventionContent.pickFreshLine(candidates, recentLines = listOf("ALPHA"))
            assertNotEquals("alpha", picked)
        }
    }

    @Test
    fun `pickFreshLine falls back to full pool when everything is recent`() {
        val candidates = listOf("alpha", "beta")
        repeat(20) {
            val picked = InterventionContent.pickFreshLine(candidates, recentLines = candidates)
            assertTrue(candidates.contains(picked))
        }
    }

    @Test
    fun `parseLeverageJson round trips simple object`() {
        val parsed = InterventionContent.parseLeverageJson("""{"Relationship status":"Single","Gym status":"Regular"}""")
        assertEquals(2, parsed.size)
        assertTrue(parsed.contains("Relationship status" to "Single"))
        assertTrue(parsed.contains("Gym status" to "Regular"))
    }

    @Test
    fun `parseLeverageJson returns empty on invalid json`() {
        assertTrue(InterventionContent.parseLeverageJson("not json at all").isEmpty())
        assertTrue(InterventionContent.parseLeverageJson("").isEmpty())
    }
}
