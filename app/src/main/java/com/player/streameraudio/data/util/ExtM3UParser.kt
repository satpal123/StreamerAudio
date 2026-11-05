package com.player.streameraudio.data.util

class ExtM3UParser {
    fun parse(input: String): List<ExtMediaEntry> {
        val entries = mutableListOf<ExtMediaEntry>()
        val lines = input.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var currentEntry: ExtMediaEntry? = null

        for (line in lines) {
            when {
                line.startsWith("#EXT-X-MEDIA") -> {
                    currentEntry = parseExtMedia(line)?.also {
                        entries.add(it)
                    }
                }
                line.startsWith("#EXT-X-STREAM-INF") -> {
                    val entry = currentEntry ?: ExtMediaEntry().also(entries::add)
                    parseStreamInf(line, entry)
                    currentEntry = entry
                }
                line.startsWith("http") || line.startsWith("https") -> {
                    currentEntry?.apply {
                        playlistUrl = line
                        currentEntry = null // Reset after capturing URL
                    }
                }
            }
        }

        return entries
    }

    private fun parseExtMedia(line: String): ExtMediaEntry? {
        val attributes = parseAttributes(line.substringAfter(':', ""))
        val entry = ExtMediaEntry()

        entry.type = attributes["TYPE"]
        entry.groupId = attributes["GROUP-ID"]
        entry.name = attributes["NAME"]
        entry.autoSelect = attributes["AUTOSELECT"].equals("YES", ignoreCase = true)
        entry.default = attributes["DEFAULT"].equals("YES", ignoreCase = true)

        return entry
    }

    private fun parseStreamInf(line: String, entry: ExtMediaEntry) {
        val attributes = parseAttributes(line.substringAfter(':', ""))
        entry.name = entry.name
            ?: attributes["NAME"]
            ?: attributes["IVS-NAME"]
            ?: attributes["STABLE-VARIANT-ID"]
        entry.bandwidth = attributes["BANDWIDTH"]?.toLongOrNull()
        entry.resolution = attributes["RESOLUTION"]
        entry.codecs = attributes["CODECS"]
        entry.video = attributes["VIDEO"]
        entry.frameRate = attributes["FRAME-RATE"]?.toDoubleOrNull()
    }

    /**
     * HLS attribute lists are comma-separated, except that quoted values (notably CODECS)
     * may themselves contain commas. A tiny state machine is more reliable than splitting.
     */
    private fun parseAttributes(value: String): Map<String, String> {
        val attributes = linkedMapOf<String, String>()
        var start = 0
        var quoted = false

        fun consume(end: Int) {
            val part = value.substring(start, end).trim()
            val separator = part.indexOf('=')
            if (separator > 0) {
                val key = part.substring(0, separator).trim()
                val rawValue = part.substring(separator + 1).trim()
                attributes[key] = rawValue.removeSurrounding("\"")
            }
        }

        value.forEachIndexed { index, character ->
            when (character) {
                '"' -> quoted = !quoted
                ',' -> if (!quoted) {
                    consume(index)
                    start = index + 1
                }
            }
        }
        consume(value.length)
        return attributes
    }
}

data class ExtMediaEntry(
    var type: String? = null,
    var groupId: String? = null,
    var name: String? = null,
    var autoSelect: Boolean = false,
    var default: Boolean = false,
    var bandwidth: Long? = null,  // Changed from Int to Long (bandwidth can be large)
    var resolution: String? = null,
    var codecs: String? = null,
    var video: String? = null,
    var frameRate: Double? = null,
    var playlistUrl: String? = null
)
