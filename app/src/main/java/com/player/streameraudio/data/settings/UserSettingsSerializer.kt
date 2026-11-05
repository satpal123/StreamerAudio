package com.player.streameraudio.data.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

class UserSettingsSerializer : Serializer<UserSettings> {
    override val defaultValue: UserSettings = UserSettings()

    override suspend fun readFrom(input: InputStream): UserSettings = try {
        Json.decodeFromString(UserSettings.serializer(), input.readBytes().decodeToString())
    } catch (error: SerializationException) {
        throw CorruptionException("Unable to read user settings", error)
    }

    override suspend fun writeTo(t: UserSettings, output: OutputStream) {
        output.write(Json.encodeToString(UserSettings.serializer(), t).encodeToByteArray())
    }
}
