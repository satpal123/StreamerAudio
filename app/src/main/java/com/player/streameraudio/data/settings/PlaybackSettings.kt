package com.player.streameraudio.data.settings

import kotlinx.serialization.Serializable

@Serializable
data class ProxyEndpoint(
    val id: String,
    val name: String,
    val baseUrl: String
)

@Serializable
enum class PlaybackMode { DIRECT, PROXY }

@Serializable
data class PlaybackRouting(
    val mode: PlaybackMode = PlaybackMode.DIRECT,
    val selectedProxyId: String? = null,
    val fallbackEnabled: Boolean = false
)

@Serializable
data class LastPlayedStream(
    val mediaId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String? = null,
    val userLogin: String? = null,
    val startedAtEpochMs: Long? = null
)

@Serializable
data class UserSettings(
    val proxies: List<ProxyEndpoint> = emptyList(),
    val routing: PlaybackRouting = PlaybackRouting(),
    val legacyProxyNoticePending: Boolean = false,
    val sleepTimerDeadlineEpochMs: Long? = null,
    val lastPlayedStream: LastPlayedStream? = null
)

sealed interface ProxyValidationResult {
    data class Valid(val normalizedUrl: String) : ProxyValidationResult
    data class Invalid(val message: String) : ProxyValidationResult
}

object ProxyEndpointValidator {
    fun validate(name: String, rawUrl: String): ProxyValidationResult {
        if (name.trim().isEmpty()) return ProxyValidationResult.Invalid("Enter a proxy name")
        if (name.trim().length > 50) return ProxyValidationResult.Invalid("Proxy name must be 50 characters or fewer")

        val url = try {
            java.net.URI(rawUrl.trim())
        } catch (_: Exception) {
            return ProxyValidationResult.Invalid("Enter a valid HTTPS URL")
        }
        if (!url.scheme.equals("https", ignoreCase = true)) {
            return ProxyValidationResult.Invalid("Only HTTPS proxy URLs are supported")
        }
        if (url.host.isNullOrBlank()) return ProxyValidationResult.Invalid("The proxy URL must include a host")
        if (!url.userInfo.isNullOrBlank()) return ProxyValidationResult.Invalid("Credentials are not allowed in proxy URLs")
        if (!url.query.isNullOrBlank() || !url.fragment.isNullOrBlank()) {
            return ProxyValidationResult.Invalid("Remove query parameters and fragments from the proxy URL")
        }

        val normalized = java.net.URI(
            "https",
            null,
            url.host.lowercase(),
            url.port,
            url.path.orEmpty().trimEnd('/').ifEmpty { null },
            null,
            null
        ).toString()
        return ProxyValidationResult.Valid(normalized)
    }
}
