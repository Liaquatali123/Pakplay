package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeSorterTest {

    @Test
    fun test50EpisodesSortedLineWise() {
        // Create 50 episode titles in random/shuffled order
        val episodes = (1..50).map { "My Favorite Anime - Episode $it.mp4" }.shuffled()

        // Sort using EpisodeSorter
        val sorted = episodes.sortedWith { a, b -> EpisodeSorter.compare(a, b) }

        // Expected is 1 to 50 in exact line-wise sequence
        val expected = (1..50).map { "My Favorite Anime - Episode $it.mp4" }
        assertEquals(expected, sorted)
    }

    @Test
    fun testLexicographicalPitfallAvoided() {
        // In standard ASCII sort: "Episode 10" comes before "Episode 2"
        val titles = listOf("Episode 10.mp4", "Episode 2.mp4", "Episode 1.mp4", "Episode 20.mp4")
        val sorted = titles.sortedWith { a, b -> EpisodeSorter.compare(a, b) }

        assertEquals(listOf("Episode 1.mp4", "Episode 2.mp4", "Episode 10.mp4", "Episode 20.mp4"), sorted)
    }

    @Test
    fun testSeasonAndEpisodeSorting() {
        val titles = listOf(
            "Show.S02E01.1080p.mkv",
            "Show.S01E10.1080p.mkv",
            "Show.S01E02.1080p.mkv",
            "Show.S01E01.1080p.mkv"
        )
        val sorted = titles.sortedWith { a, b -> EpisodeSorter.compare(a, b) }

        assertEquals(
            listOf(
                "Show.S01E01.1080p.mkv",
                "Show.S01E02.1080p.mkv",
                "Show.S01E10.1080p.mkv",
                "Show.S02E01.1080p.mkv"
            ),
            sorted
        )
    }

    @Test
    fun testReverseEpisodeSorting() {
        val episodes = (1..10).map { "Episode $it.mp4" }.shuffled()
        val reverseSorted = episodes.sortedWith { a, b -> EpisodeSorter.compare(b, a) }

        val expected = (10 downTo 1).map { "Episode $it.mp4" }
        assertEquals(expected, reverseSorted)
    }

    @Test
    fun testEpisodeBadgeExtraction() {
        assertEquals("S1:E5", EpisodeSorter.getEpisodeBadge("Breaking.Bad.S01E05.720p.mkv"))
        assertEquals("Ep. 12", EpisodeSorter.getEpisodeBadge("Attack on Titan - Episode 12 [1080p]"))
        assertEquals("Ep. 3", EpisodeSorter.getEpisodeBadge("Naruto Shippuden Ep 3 Eng Sub"))
        assertEquals("#50", EpisodeSorter.getEpisodeBadge("50.mp4"))
    }
}
