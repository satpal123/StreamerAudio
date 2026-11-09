package com.player.streameraudio.data.update

import android.content.Context
import android.os.Build
import com.player.streameraudio.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

data class DirectUpdate(
    val versionName: String,
    val downloadUrl: String,
    val releaseUrl: String
)

sealed interface DirectUpdateResult {
    data class Available(val update: DirectUpdate) : DirectUpdateResult
    data object UpToDate : DirectUpdateResult
}

@Singleton
class GitHubUpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient
) {
    fun isInstalledFromGooglePlay(): Boolean {
        val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
        }
        return installer == GOOGLE_PLAY_PACKAGE
    }

    fun check(): DirectUpdateResult {
        val request = Request.Builder()
            .url(LATEST_RELEASE_URL)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "StreamerAudio/${BuildConfig.VERSION_NAME}")
            .build()

        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Update check failed (${response.code})" }
            val body = response.body.string()
            val release = Json.parseToJsonElement(body) as? JsonObject
                ?: error("GitHub returned an invalid release")
            val version = release.string("tag_name").removePrefix("v").removePrefix("V")
            val releaseUrl = release.string("html_url")
            val apkUrl = (release["assets"] as? JsonArray)
                ?.mapNotNull { it as? JsonObject }
                ?.firstOrNull { it.stringOrNull("name")?.endsWith(".apk", ignoreCase = true) == true }
                ?.stringOrNull("browser_download_url")
                ?: releaseUrl

            return if (VersionComparator.isNewer(version, BuildConfig.VERSION_NAME)) {
                DirectUpdateResult.Available(DirectUpdate(version, apkUrl, releaseUrl))
            } else {
                DirectUpdateResult.UpToDate
            }
        }
    }

    private fun JsonObject.string(name: String): String =
        stringOrNull(name)?.takeIf { it.isNotBlank() } ?: error("GitHub release is missing $name")

    private fun JsonObject.stringOrNull(name: String): String? =
        runCatching { get(name)?.jsonPrimitive?.content }.getOrNull()

    private companion object {
        const val GOOGLE_PLAY_PACKAGE = "com.android.vending"
        const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/satpal123/StreamerAudio/releases/latest"
    }
}

object VersionComparator {
    fun isNewer(candidate: String, current: String): Boolean {
        val candidateParts = numericParts(candidate)
        val currentParts = numericParts(current)
        val count = maxOf(candidateParts.size, currentParts.size)
        for (index in 0 until count) {
            val left = candidateParts.getOrElse(index) { 0 }
            val right = currentParts.getOrElse(index) { 0 }
            if (left != right) return left > right
        }
        return false
    }

    private fun numericParts(value: String): List<Int> = value
        .substringBefore('-')
        .removePrefix("v")
        .removePrefix("V")
        .split('.')
        .map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
}
