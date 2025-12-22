package com.player.streameraudio.data.util

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit tests for ExtM3UParser
 * Tests various M3U playlist parsing scenarios including:
 * - Basic EXT-X-MEDIA parsing
 * - STREAM-INF parsing
 * - Playlist URL extraction
 * - Edge cases and malformed input
 */
class ExtM3UParserTest {

    private lateinit var parser: ExtM3UParser

    @Before
    fun setUp() {
        parser = ExtM3UParser()
    }

    // ========== Basic EXT-X-MEDIA Parsing Tests ==========

    @Test
    fun `parse simple EXT-X-MEDIA entry`() {
        val input = """
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio-group",NAME="English"
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(1, result.size)
        assertEquals("AUDIO", result[0].type)
        assertEquals("audio-group", result[0].groupId)
        assertEquals("English", result[0].name)
    }

    @Test
    fun `parse EXT-X-MEDIA with all attributes`() {
        val input = """
            #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="video-group",NAME="HD Stream",AUTOSELECT=YES,DEFAULT=YES
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(1, result.size)
        with(result[0]) {
            assertEquals("VIDEO", type)
            assertEquals("video-group", groupId)
            assertEquals("HD Stream", name)
            assertTrue(autoSelect)
            assertTrue(default)
        }
    }

    @Test
    fun `parse EXT-X-MEDIA with AUTOSELECT=NO and DEFAULT=NO`() {
        val input = """
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="Spanish",AUTOSELECT=NO,DEFAULT=NO
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(1, result.size)
        assertFalse(result[0].autoSelect)
        assertFalse(result[0].default)
    }

    @Test
    fun `parse EXT-X-MEDIA with case insensitive boolean values`() {
        val input = """
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="Test",AUTOSELECT=yes,DEFAULT=Yes
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(1, result.size)
        assertTrue(result[0].autoSelect)
        assertTrue(result[0].default)
    }

    // ========== EXT-X-STREAM-INF Parsing Tests ==========

    @Test
    fun `parse EXT-X-MEDIA with STREAM-INF`() {
        val input = """
            #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="video",NAME="720p"
            #EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1280x720,CODECS="avc1.64001f,mp4a.40.2"
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(1, result.size)
        with(result[0]) {
            assertEquals("VIDEO", type)
            assertEquals(2500000L, bandwidth)
            assertEquals("1280x720", resolution)
            assertEquals("avc1.64001f,mp4a.40.2", codecs)
        }
    }

    @Test
    fun `parse STREAM-INF with all attributes`() {
        val input = """
            #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="video",NAME="1080p"
            #EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080,CODECS="avc1.640028",VIDEO="video-1",FRAME-RATE=60.0
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(1, result.size)
        with(result[0]) {
            assertEquals(5000000L, bandwidth)
            assertEquals("1920x1080", resolution)
            assertEquals("avc1.640028", codecs)
            assertEquals("video-1", video)
            assertEquals(60.0, frameRate!!, 0.001)
        }
    }

    @Test
    fun `parse STREAM-INF with decimal frame rate`() {
        val input = """
            #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="video",NAME="Test"
            #EXT-X-STREAM-INF:BANDWIDTH=3000000,FRAME-RATE=29.97
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(1, result.size)
        assertEquals(29.97, result[0].frameRate!!, 0.001)
    }

    @Test
    fun `parse STREAM-INF with large bandwidth value`() {
        val input = """
            #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="video",NAME="4K"
            #EXT-X-STREAM-INF:BANDWIDTH=15000000
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(1, result.size)
        assertEquals(15000000L, result[0].bandwidth)
    }

    // ========== Playlist URL Tests ==========

    @Test
    fun `parse entry with http playlist URL`() {
        val input = """
            #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="video",NAME="Stream"
            http://example.com/playlist.m3u8
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(1, result.size)
        assertEquals("http://example.com/playlist.m3u8", result[0].playlistUrl)
    }

    @Test
    fun `parse entry with https playlist URL`() {
        val input = """
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="Audio Stream"
            https://secure.example.com/stream/audio.m3u8
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(1, result.size)
        assertEquals("https://secure.example.com/stream/audio.m3u8", result[0].playlistUrl)
    }

    @Test
    fun `parse complete entry with all components`() {
        val input = """
            #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="video-hd",NAME="High Quality",AUTOSELECT=YES,DEFAULT=YES
            #EXT-X-STREAM-INF:BANDWIDTH=8000000,RESOLUTION=1920x1080,CODECS="avc1.640028,mp4a.40.2",VIDEO="video-1",FRAME-RATE=60.0
            https://cdn.example.com/stream/hd.m3u8
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(1, result.size)
        with(result[0]) {
            assertEquals("VIDEO", type)
            assertEquals("video-hd", groupId)
            assertEquals("High Quality", name)
            assertTrue(autoSelect)
            assertTrue(default)
            assertEquals(8000000L, bandwidth)
            assertEquals("1920x1080", resolution)
            assertEquals("avc1.640028,mp4a.40.2", codecs)
            assertEquals("video-1", video)
            assertEquals(60.0, frameRate!!, 0.001)
            assertEquals("https://cdn.example.com/stream/hd.m3u8", playlistUrl)
        }
    }

    // ========== Multiple Entries Tests ==========

    @Test
    fun `parse multiple EXT-X-MEDIA entries`() {
        val input = """
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="English"
            https://example.com/audio/en.m3u8

            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="Spanish"
            https://example.com/audio/es.m3u8

            #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="video",NAME="HD"
            #EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080
            https://example.com/video/hd.m3u8
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(3, result.size)

        assertEquals("AUDIO", result[0].type)
        assertEquals("English", result[0].name)
        assertEquals("https://example.com/audio/en.m3u8", result[0].playlistUrl)

        assertEquals("AUDIO", result[1].type)
        assertEquals("Spanish", result[1].name)
        assertEquals("https://example.com/audio/es.m3u8", result[1].playlistUrl)

        assertEquals("VIDEO", result[2].type)
        assertEquals("HD", result[2].name)
        assertEquals(5000000L, result[2].bandwidth)
        assertEquals("https://example.com/video/hd.m3u8", result[2].playlistUrl)
    }

    @Test
    fun `parse entries with extra whitespace and empty lines`() {
        val input = """

            #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="video",NAME="Stream 1"

            https://example.com/stream1.m3u8


            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="Stream 2"
            https://example.com/stream2.m3u8

        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(2, result.size)
        assertEquals("Stream 1", result[0].name)
        assertEquals("Stream 2", result[1].name)
    }

    // ========== Edge Cases and Error Handling Tests ==========

    @Test
    fun `parse empty input returns empty list`() {
        val result = parser.parse("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse whitespace only input returns empty list`() {
        val result = parser.parse("   \n   \n   ")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse input with only STREAM-INF without EXT-X-MEDIA`() {
        val input = """
            #EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080
            https://example.com/stream.m3u8
        """.trimIndent()

        val result = parser.parse(input)

        // Should not create entries without EXT-X-MEDIA
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse EXT-X-MEDIA with minimal attributes`() {
        val input = """
            #EXT-X-MEDIA:TYPE=VIDEO
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(1, result.size)
        assertEquals("VIDEO", result[0].type)
        assertNull(result[0].groupId)
        assertNull(result[0].name)
    }

    @Test
    fun `parse EXT-X-MEDIA without TYPE attribute`() {
        val input = """
            #EXT-X-MEDIA:GROUP-ID="audio",NAME="English"
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(1, result.size)
        assertNull(result[0].type)
        assertEquals("audio", result[0].groupId)
        assertEquals("English", result[0].name)
    }

    @Test
    fun `parse STREAM-INF with invalid bandwidth value`() {
        val input = """
            #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="video",NAME="Test"
            #EXT-X-STREAM-INF:BANDWIDTH=invalid,RESOLUTION=1920x1080
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(1, result.size)
        assertNull(result[0].bandwidth)
        assertEquals("1920x1080", result[0].resolution)
    }

    @Test
    fun `parse STREAM-INF with invalid frame rate value`() {
        val input = """
            #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="video",NAME="Test"
            #EXT-X-STREAM-INF:FRAME-RATE=notanumber
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(1, result.size)
        assertNull(result[0].frameRate)
    }

    @Test
    fun `parse attributes with extra quotes and whitespace`() {
        val input = """
            #EXT-X-MEDIA:TYPE=AUDIO , GROUP-ID = "audio-group" , NAME = "English Audio"
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(1, result.size)
        assertEquals("AUDIO", result[0].type)
        assertEquals("audio-group", result[0].groupId)
        assertEquals("English Audio", result[0].name)
    }

    @Test
    fun `parse EXT-X-MEDIA with URL but URL gets assigned correctly`() {
        val input = """
            #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="video",NAME="Stream 1"
            https://example.com/first.m3u8
            #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="video",NAME="Stream 2"
            https://example.com/second.m3u8
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(2, result.size)
        assertEquals("Stream 1", result[0].name)
        assertEquals("https://example.com/first.m3u8", result[0].playlistUrl)
        assertEquals("Stream 2", result[1].name)
        assertEquals("https://example.com/second.m3u8", result[1].playlistUrl)
    }

    @Test
    fun `parse attributes without quotes`() {
        val input = """
            #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID=video-group,NAME=StreamName
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(1, result.size)
        assertEquals("VIDEO", result[0].type)
        assertEquals("video-group", result[0].groupId)
        assertEquals("StreamName", result[0].name)
    }

    @Test
    fun `parse URL without preceding EXT-X-MEDIA is ignored`() {
        val input = """
            https://example.com/orphan.m3u8
            #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="video",NAME="Valid Stream"
            https://example.com/valid.m3u8
        """.trimIndent()

        val result = parser.parse(input)

        // Only the entry with EXT-X-MEDIA should be captured
        assertEquals(1, result.size)
        assertEquals("Valid Stream", result[0].name)
        assertEquals("https://example.com/valid.m3u8", result[0].playlistUrl)
    }

    @Test
    fun `parse special characters in attribute values`() {
        val input = """
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio-group-1",NAME="English (US) - Main Audio"
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(1, result.size)
        assertEquals("English (US) - Main Audio", result[0].name)
    }

    @Test
    fun `parse codecs with complex values`() {
        val input = """
            #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="video",NAME="Test"
            #EXT-X-STREAM-INF:CODECS="avc1.64001f,mp4a.40.2,vp09.00.10.08"
        """.trimIndent()

        val result = parser.parse(input)

        assertEquals(1, result.size)
        assertEquals("avc1.64001f,mp4a.40.2,vp09.00.10.08", result[0].codecs)
    }

    // ========== Data Class Tests ==========

    @Test
    fun `ExtMediaEntry default values are correct`() {
        val entry = ExtMediaEntry()

        assertNull(entry.type)
        assertNull(entry.groupId)
        assertNull(entry.name)
        assertFalse(entry.autoSelect)
        assertFalse(entry.default)
        assertNull(entry.bandwidth)
        assertNull(entry.resolution)
        assertNull(entry.codecs)
        assertNull(entry.video)
        assertNull(entry.frameRate)
        assertNull(entry.playlistUrl)
    }

    @Test
    fun `ExtMediaEntry can be created with all parameters`() {
        val entry = ExtMediaEntry(
            type = "VIDEO",
            groupId = "video-group",
            name = "HD Stream",
            autoSelect = true,
            default = true,
            bandwidth = 5000000L,
            resolution = "1920x1080",
            codecs = "avc1.640028",
            video = "video-1",
            frameRate = 60.0,
            playlistUrl = "https://example.com/stream.m3u8"
        )

        assertEquals("VIDEO", entry.type)
        assertEquals("video-group", entry.groupId)
        assertEquals("HD Stream", entry.name)
        assertTrue(entry.autoSelect)
        assertTrue(entry.default)
        assertEquals(5000000L, entry.bandwidth)
        assertEquals("1920x1080", entry.resolution)
        assertEquals("avc1.640028", entry.codecs)
        assertEquals("video-1", entry.video)
        assertEquals(60.0, entry.frameRate!!, 0.001)
        assertEquals("https://example.com/stream.m3u8", entry.playlistUrl)
    }
}
