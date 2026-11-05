package com.player.streameraudio.data.settings

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedStreamTest {

    @Test
    fun `stable key normalizes channel login`() {
        val stream = SavedStream(
            mediaId = "123",
            userLogin = "  TwitchDev  ",
            title = "Live",
            artist = "TwitchDev"
        )

        assertEquals("twitchdev", stream.stableKey)
    }

    @Test
    fun `stable key falls back to media id`() {
        val stream = SavedStream(
            mediaId = " ChannelName ",
            userLogin = " ",
            title = "Live",
            artist = "Channel"
        )

        assertEquals("channelname", stream.stableKey)
    }

    @Test
    fun `older settings decode with empty library collections`() {
        val settings = Json.decodeFromString<UserSettings>("{}")

        assertTrue(settings.favoriteStreams.isEmpty())
        assertTrue(settings.recentStreams.isEmpty())
        assertTrue(!settings.volumeLevelingEnabled)
    }
}
