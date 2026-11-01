package com.backend.movie.service

import kotlin.test.Test
import kotlin.test.assertEquals

class SearchTests {

    @Test
    fun likePatternOfBlankTerm() {
        assertEquals("", likePattern("   "))
    }

    @Test
    fun likePatternTrimsTerm() {
        assertEquals("%heat%", likePattern("  heat "))
    }

    @Test
    fun likePatternEscapesWildcards() {
        // Input: 100%_\   Pattern: %100\%\_\\%
        assertEquals("""%100\%\_\\%""", likePattern("""100%_\"""))
    }

    @Test
    fun offsetOfHugePage() {
        assertEquals(Int.MAX_VALUE.toLong() * 100, offsetOf(Int.MAX_VALUE, 100))
    }
}
