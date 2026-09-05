package com.example.util

import java.util.Locale

/**
 * Natural and Episode Order Comparator.
 *
 * Ensures that episodic media (e.g., Episode 1, Episode 2, ... Episode 9, Episode 10, ... Episode 50)
 * are sorted in true chronological / sequential order rather than flawed lexicographical order
 * where "Episode 10" would precede "Episode 2", or where out-of-order downloads scatter episodes.
 */
object EpisodeSorter {

    private val SEASON_EPISODE_REGEX = Regex("""(?i)\b(?:s|season)[.\s_-]*(\d+)[.\s_-]*(?:e|ep|episode)[.\s_-]*(\d+(?:\.\d+)?)\b""")
    private val X_PATTERN_REGEX = Regex("""(?i)\b(\d{1,2})x(\d{1,3})\b""")
    private val EXPLICIT_EP_REGEX = Regex("""(?i)\b(?:ep|episode|eps|e|part|pt|ch|chapter)[.\s_-]*#?\s*(\d+(?:\.\d+)?)\b""")
    private val HASH_EP_REGEX = Regex("""#\s*(\d+(?:\.\d+)?)""")
    private val BRACKETED_EP_REGEX = Regex("""[\[(]\s*(\d{1,4})\s*[\])]""")
    private val DELIMITED_NUMBER_REGEX = Regex("""(?:^|[\s_.-])(\d{1,4})(?:[\s_.-]|$)""")

    data class EpisodeData(
        val season: Double? = null,
        val episode: Double? = null,
        val rawNumber: Double? = null
    )

    fun extractEpisodeData(title: String): EpisodeData {
        // 1. Season + Episode: S01E02 or Season 1 Episode 2
        SEASON_EPISODE_REGEX.find(title)?.let { match ->
            val s = match.groupValues[1].toDoubleOrNull()
            val e = match.groupValues[2].toDoubleOrNull()
            if (e != null) return EpisodeData(season = s, episode = e)
        }

        // 2. 1x02 pattern
        X_PATTERN_REGEX.find(title)?.let { match ->
            val s = match.groupValues[1].toDoubleOrNull()
            val e = match.groupValues[2].toDoubleOrNull()
            if (e != null) return EpisodeData(season = s, episode = e)
        }

        // 3. Explicit Ep/Episode: "Episode 10", "Ep. 5", "Part 2"
        EXPLICIT_EP_REGEX.find(title)?.let { match ->
            val ep = match.groupValues[1].toDoubleOrNull()
            if (ep != null) return EpisodeData(episode = ep)
        }

        // 4. #5
        HASH_EP_REGEX.find(title)?.let { match ->
            val ep = match.groupValues[1].toDoubleOrNull()
            if (ep != null) return EpisodeData(episode = ep)
        }

        // 5. [01] or (01)
        BRACKETED_EP_REGEX.find(title)?.let { match ->
            val ep = match.groupValues[1].toDoubleOrNull()
            if (ep != null) return EpisodeData(episode = ep)
        }

        // 6. Delimited number: "Show - 05 - Title"
        DELIMITED_NUMBER_REGEX.find(title)?.let { match ->
            val num = match.groupValues[1].toDoubleOrNull()
            if (num != null) return EpisodeData(rawNumber = num)
        }

        return EpisodeData()
    }

    /**
     * Extracts a display label for the episode if recognized (e.g., "Ep. 5", "S1:E2", "#12").
     */
    fun getEpisodeBadge(title: String): String? {
        val data = extractEpisodeData(title)
        return when {
            data.season != null && data.episode != null -> {
                val s = data.season.toInt()
                val e = if (data.episode % 1.0 == 0.0) data.episode.toInt().toString() else data.episode.toString()
                "S$s:E$e"
            }
            data.episode != null -> {
                val ep = if (data.episode % 1.0 == 0.0) data.episode.toInt().toString() else data.episode.toString()
                "Ep. $ep"
            }
            data.rawNumber != null -> {
                val num = data.rawNumber.toInt()
                "#$num"
            }
            else -> null
        }
    }

    /**
     * Compare two filenames/titles in human natural episode order.
     * Ascending: Episode 1 -> Episode 2 -> Episode 10 -> Episode 50.
     */
    fun compare(title1: String, title2: String): Int {
        val data1 = extractEpisodeData(title1)
        val data2 = extractEpisodeData(title2)

        // Compare seasons if both have seasons
        if (data1.season != null && data2.season != null && data1.season != data2.season) {
            return data1.season.compareTo(data2.season)
        }

        // Compare explicit episode numbers if both have them
        val ep1 = data1.episode ?: data1.rawNumber
        val ep2 = data2.episode ?: data2.rawNumber
        if (ep1 != null && ep2 != null && ep1 != ep2) {
            return ep1.compareTo(ep2)
        }

        // Fallback to Natural Alphanumeric comparison on the whole string
        return naturalCompare(title1, title2)
    }

    /**
     * Natural Alphanumeric String comparison.
     * Treats runs of digits as numerical values rather than individual ASCII characters.
     */
    fun naturalCompare(s1: String, s2: String): Int {
        var i1 = 0
        var i2 = 0
        val len1 = s1.length
        val len2 = s2.length

        while (i1 < len1 && i2 < len2) {
            val c1 = s1[i1]
            val c2 = s2[i2]

            if (c1.isDigit() && c2.isDigit()) {
                // Read continuous digits from s1
                var end1 = i1
                while (end1 < len1 && s1[end1].isDigit()) end1++
                // Read continuous digits from s2
                var end2 = i2
                while (end2 < len2 && s2[end2].isDigit()) end2++

                val strNum1 = s1.substring(i1, end1).trimStart('0')
                val strNum2 = s2.substring(i2, end2).trimStart('0')

                val nLen1 = strNum1.length
                val nLen2 = strNum2.length

                if (nLen1 != nLen2) {
                    return nLen1.compareTo(nLen2)
                }

                val cmp = strNum1.compareTo(strNum2)
                if (cmp != 0) {
                    return cmp
                }

                // Tie-breaker on count of leading zeroes
                val rawLen1 = end1 - i1
                val rawLen2 = end2 - i2
                if (rawLen1 != rawLen2) {
                    return rawLen1.compareTo(rawLen2)
                }

                i1 = end1
                i2 = end2
            } else {
                val cmp = c1.lowercaseChar().compareTo(c2.lowercaseChar())
                if (cmp != 0) {
                    return cmp
                }
                i1++
                i2++
            }
        }
        return len1.compareTo(len2)
    }
}
