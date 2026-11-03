package com.player.streameraudio.data.auth

import android.content.Context
import com.player.streameraudio.data.util.Constants
import com.player.streameraudio.data.util.SecurePreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegacyAuthMigrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenStore: TokenStore
) {
    fun migrateOnce(): Boolean {
        val marker = context.getSharedPreferences("v2_migrations", Context.MODE_PRIVATE)
        if (marker.getBoolean("auth_complete", false)) return false
        var hadLegacyProxy = false
        runCatching {
            val legacy = SecurePreferences.getEncryptedSharedPreferences(context)
            hadLegacyProxy = !legacy.getString(Constants.PREF_SELECTED_PROXY, null).isNullOrBlank()
            if (tokenStore.read() == null) {
                legacy.getString(Constants.PREF_ACCESS_TOKEN, null)?.takeIf { it.isNotBlank() }?.let { token ->
                    tokenStore.write(AuthSession(token, legacy.getString(Constants.PREF_USER_ID, null), legacy.getString(Constants.PREF_USER_LOGIN, null)))
                }
            }
            legacy.edit().clear().apply()
        }
        marker.edit().putBoolean("auth_complete", true).apply()
        return hadLegacyProxy
    }
}
