package com.player.streameraudio.service

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.player.streameraudio.data.api.TwitchGqlService
import com.player.streameraudio.data.util.Constants
import com.player.streameraudio.data.util.ExtM3UParser
import com.player.streameraudio.data.util.ResponseState
import com.player.streameraudio.data.util.SecurePreferences
import com.player.streameraudio.domain.usecase.GetBackendCategoriesUseCase
import com.player.streameraudio.domain.usecase.GetBackendChannelsUseCase
import com.player.streameraudio.domain.usecase.GetBackendProfileImageUseCase
import com.player.streameraudio.domain.usecase.GetBackendTopStreamsUseCase
import com.player.streameraudio.domain.usecase.GetStreamsByGameUseCase
import com.player.streameraudio.domain.usecase.GetTopGamesUseCase
import com.player.streameraudio.domain.usecase.GetTopStreamsUseCase
import com.player.streameraudio.domain.usecase.GetTwitchStreamsDetailsUseCase
import com.player.streameraudio.domain.usecase.GetTwitchStreamsUseCase
import com.player.streameraudio.domain.usecase.GetTwitchUserUseCase
import com.player.streameraudio.domain.usecase.SearchTwitchChannelsUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.future
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import android.util.Log as AndroidLog


class CustomMediaLibrarySessionCallback @Inject constructor(
    @ApplicationContext private val context: Context,
    private val twitchUserUseCase: GetTwitchUserUseCase,
    private val twitchStreamsUseCase: GetTwitchStreamsUseCase,
    private val twitchStreamsDetailsUseCase: GetTwitchStreamsDetailsUseCase,
    private val getTopStreamsUseCase: GetTopStreamsUseCase,
    private val getTopGamesUseCase: GetTopGamesUseCase,
    private val getStreamsByGameUseCase: GetStreamsByGameUseCase,
    private val searchTwitchChannelsUseCase: SearchTwitchChannelsUseCase,
    private val getBackendCategoriesUseCase: GetBackendCategoriesUseCase,
    private val getBackendChannelsUseCase: GetBackendChannelsUseCase,
    private val getBackendTopStreamsUseCase: GetBackendTopStreamsUseCase,
    private val getBackendProfileImageUseCase: GetBackendProfileImageUseCase
) : MediaLibrarySession.Callback {

    private var streamUrl: String? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    private val securePrefs: SharedPreferences by lazy {
        SecurePreferences.getEncryptedSharedPreferences(context)
    }

    // Cache browse metadata by mediaId so Now Playing shows correct values.
    private data class CachedPlayable(
        val title: String,
        val artist: String,
        val artwork: Uri?,      // list artwork (thumbnail)
        val userLogin: String?,
        val startedAtMs: Long?
    )

    private val playableCache = ConcurrentHashMap<String, CachedPlayable>()

    // Cache streamer avatars by login (used on playback / Now Playing)
    private val avatarCache = ConcurrentHashMap<String, Uri>()

    private suspend fun getAvatarUriUnauth(login: String): Uri? {
        val key = login.trim().lowercase()
        avatarCache[key]?.let { return it }

        // Best-effort so playback doesn't hang on artwork
        val uri = withTimeoutOrNull(2000L) { fetchProfileImageUnauthenticated(key) }
        if (uri != null && uri != Uri.EMPTY) {
            avatarCache[key] = uri
            return uri
        }
        return null
    }

    fun clearCachesForLogout() {
        playableCache.clear()
        avatarCache.clear()
        cursorCache.clear()
        streamUrl = null
    }

    companion object {
        const val ROOT_AUTH = "root_auth"
        const val ROOT_GUEST = "root_guest"

        const val ROOT = "root"

        const val FOLLOWED = "FOLLOWED"
        const val TOP_GAMES = "TOP_GAMES"
        const val TOP_STREAMS = "TOP_STREAMS"
        const val SEARCH = "search"

        private const val GAME_PREFIX = "game:"
        private const val SEARCH_PREFIX = "search:"

        private const val EXTRA_STARTED_AT_MS = "startedAtMs"

        // Hard cap so we don't load huge lists even if pageSize is very large.
        private const val HARD_MAX_ITEMS = 150
        private const val DEFAULT_PAGE_SIZE = 50
    }

    // Cache cursors for pagination (parentId -> list of cursors by page)
    private val cursorCache = ConcurrentHashMap<String, MutableList<String?>>()

    // -------------------- Library Root --------------------

    @OptIn(UnstableApi::class)
    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {

        // âœ… Always return the same root id so Android Auto subscriptions remain valid
        val rootExtras = Bundle().apply {
            putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED", true)
            putInt(
                MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
            )
            putInt(
                MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
                MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
            )
        }
        val libraryParams = MediaLibraryService.LibraryParams.Builder()
            .setExtras(rootExtras)
            .build()

        val rootItem = MediaItem.Builder()
            .setMediaId(ROOT)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Streamer Audio")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

        return Futures.immediateFuture(LibraryResult.ofItem(rootItem, libraryParams))
    }

    // -------------------- Search --------------------

    @OptIn(UnstableApi::class)
    override fun onSearch(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<Void>> {
        AndroidLog.d("MediaSession", "onSearch called with query: '$query'")
        session.notifySearchResultChanged(browser, query, 0, params)
        return Futures.immediateFuture(LibraryResult.ofVoid())
    }

    @OptIn(UnstableApi::class)
    override fun onGetSearchResult(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        coroutineScope.future(Dispatchers.IO) {
            val accessToken = securePrefs.getString(Constants.PREF_ACCESS_TOKEN, null)
            val bearer = accessToken?.takeIf { it.isNotBlank() }?.let { "Bearer $it" }

            if (bearer == null) {
                future.set(LibraryResult.ofItemList(emptyList(), params))
                return@future
            }

            val effectivePageSize = normalizePageSize(pageSize)
            val maxPlayable = effectivePageSize

            val state = awaitNonLoading(searchTwitchChannelsUseCase(bearer, Constants.TWITCH_CLIENT_ID, query, null))
            when (state) {
                is ResponseState.Error -> future.set(LibraryResult.ofItemList(emptyList(), params))
                is ResponseState.Success -> {
                    val (channels, _) = state.data ?: Pair(emptyList(), null)
                    val items = mutableListOf<MediaItem>()
                    channels.filter { it.isLive }.forEach { c ->
                        if (items.size >= maxPlayable) return@forEach
                        val art = c.thumbnailUrl?.let { u -> try { Uri.parse(u) } catch (_: Exception) { null } }
                        items.add(
                            createPlayableStreamItem(
                                mediaId = c.id,
                                title = c.title.orEmpty().ifBlank { "Live" },
                                artist = c.displayName,
                                artwork = art
                            )
                        )
                    }
                    future.set(LibraryResult.ofItemList(items.toImmutable(), params))
                }
                else -> future.set(LibraryResult.ofItemList(emptyList(), params))
            }
        }

        return future
    }

    // -------------------- Children (Browse) --------------------

    @OptIn(UnstableApi::class)
    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {

        AndroidLog.d("MediaSession", "onGetChildren: parentId='$parentId', page=$page, pageSize=$pageSize, browserPackage=${browser.packageName}")

        val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
        coroutineScope.future(Dispatchers.IO) {

            val accessToken = securePrefs.getString(Constants.PREF_ACCESS_TOKEN, null)
            val loggedinUserId = securePrefs.getString(Constants.PREF_USER_ID, null)
            val bearer = accessToken?.takeIf { it.isNotBlank() }?.let { "Bearer $it" }

            AndroidLog.d("MediaSession", "onGetChildren auth check: token=${accessToken?.take(10)}, userId=$loggedinUserId, bearer=${bearer != null}")

            val effectivePageSize = normalizePageSize(pageSize)
            val hardCap = minOf(effectivePageSize, HARD_MAX_ITEMS).coerceAtLeast(1)

            // Root menu
            if (parentId == ROOT || parentId == ROOT_AUTH || parentId == ROOT_GUEST) {
                val nodes = mutableListOf<MediaItem>()
                if (bearer != null && !loggedinUserId.isNullOrBlank()) {
                    nodes.add(createBrowsableItem(FOLLOWED, "Following", "Live channels you follow"))
                }
                nodes.add(createBrowsableItem(TOP_GAMES, "Categories", "Browse streams by game"))
                nodes.add(createBrowsableItem(TOP_STREAMS, "Live", "Most-watched live streams"))
                if (bearer != null) nodes.add(createBrowsableItem(SEARCH, "Search", "Search live channels"))

                future.set(LibraryResult.ofItemList(nodes.toImmutable(), params))
                return@future
            }

            // Get cursor for this page (null for page 0, cached cursor for subsequent pages)
            val cursor = getCursorForPage(parentId, page)

            AndroidLog.d("MediaSession", "Loading page $page with cursor: ${cursor?.take(10)}, requested pageSize=$pageSize, normalized hardCap=$hardCap")

            // Load items - keep fetching until we have pageSize items or run out
            val allItems = mutableListOf<MediaItem>()
            var currentCursor = cursor
            var finalCursor: String? = null
            var fetchCount = 0

            // Fetch until we have enough items or no more available
            while (allItems.size < hardCap) {
                fetchCount++
                AndroidLog.d("MediaSession", "Fetch #$fetchCount: current size=${allItems.size}, need ${hardCap - allItems.size} more")

                val (items, nextCursor) = loadSinglePage(
                    parentId = parentId,
                    bearer = bearer,
                    loggedinUserId = loggedinUserId,
                    cursor = currentCursor,
                    maxItems = hardCap - allItems.size
                )

                AndroidLog.d("MediaSession", "Fetch #$fetchCount: got ${items.size} items, nextCursor=${nextCursor?.take(10)}")

                if (items.isEmpty()) {
                    AndroidLog.d("MediaSession", "Fetch #$fetchCount: No items returned, stopping")
                    break
                }

                allItems.addAll(items)
                finalCursor = nextCursor

                if (nextCursor.isNullOrBlank()) {
                    AndroidLog.d("MediaSession", "Fetch #$fetchCount: No more cursor, stopping (reached end of content)")
                    break
                }

                // Check if we've hit our target
                if (allItems.size >= hardCap) {
                    AndroidLog.d("MediaSession", "Fetch #$fetchCount: Reached hardCap of $hardCap items, stopping (more content available)")
                    break
                }

                // Continue fetching to fill the page
                AndroidLog.d("MediaSession", "Fetch #$fetchCount: Have ${allItems.size}/$hardCap items, cursor exists, continuing...")
                currentCursor = nextCursor
            }

            // Store the cursor for the next page (if more items exist)
            if (!finalCursor.isNullOrBlank()) {
                storeCursorForPage(parentId, page + 1, finalCursor)
                AndroidLog.d("MediaSession", "Stored cursor for page ${page + 1}")
            }

            AndroidLog.d("MediaSession", "Returning ${allItems.size} items for page $page (requested $pageSize, hardCap $hardCap) after $fetchCount fetches, hasMore=${!finalCursor.isNullOrBlank()}")
            future.set(LibraryResult.ofItemList(allItems.toImmutable(), params))
        }

        return future
    }

    private fun normalizePageSize(pageSize: Int): Int {
        return when {
            pageSize <= 0 -> DEFAULT_PAGE_SIZE
            // Android Auto often requests Int.MAX_VALUE meaning "give me everything"
            // Return more items so AA knows there's content
            pageSize == Int.MAX_VALUE -> HARD_MAX_ITEMS  // Return 250 items
            pageSize > HARD_MAX_ITEMS -> HARD_MAX_ITEMS  // Cap at 250
            else -> pageSize
        }
    }

    // -------------------- Cursor management for pagination --------------------

    private fun getCursorForPage(parentId: String, page: Int): String? {
        if (page == 0) return null // First page always starts with null cursor
        val cursors = cursorCache[parentId] ?: return null
        return cursors.getOrNull(page)
    }

    private fun storeCursorForPage(parentId: String, page: Int, cursor: String) {
        val cursors = cursorCache.getOrPut(parentId) { mutableListOf() }
        // Ensure the list is large enough
        while (cursors.size <= page) {
            cursors.add(null)
        }
        cursors[page] = cursor
    }

    // -------------------- Playback --------------------

    @OptIn(UnstableApi::class)
    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {

        val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
        val mediaId = mediaItems.firstOrNull()?.mediaId.toString()
        val existingUri = mediaItems.firstOrNull()?.localConfiguration?.uri

        val accessToken = securePrefs.getString(Constants.PREF_ACCESS_TOKEN, null)
        val bearer = accessToken?.takeIf { it.isNotBlank() }?.let { "Bearer $it" }

        val streamSource = Constants.getStreamSource(context)
        val selectedProxy = Constants.getProxyUrl(context)

        if (existingUri != null) {
            future.set(MediaSession.MediaItemsWithStartPosition(mediaItems, 0, 0L))
            return future
        }

        val childItems = mutableListOf<MediaItem>()
        coroutineScope.future(Dispatchers.IO) {
            try {
                // Unauthenticated playback
                if (bearer == null) {
                    val incomingMeta = mediaItems.firstOrNull()?.mediaMetadata
                    val extras = incomingMeta?.extras
                    val cached = playableCache[mediaId]

                    val streamerLogin = (
                            extras?.getString("userLogin")
                                ?: cached?.userLogin
                                ?: mediaId
                            ).trim().lowercase()

                    val title = incomingMeta?.title?.toString()
                        ?.takeIf { it.isNotBlank() }
                        ?: cached?.title
                        ?: "Live"

                    val artist = incomingMeta?.artist?.toString()
                        ?.takeIf { it.isNotBlank() }
                        ?: cached?.artist
                        ?: streamerLogin

                    val startedAtMs =
                        extras?.getLong(EXTRA_STARTED_AT_MS)?.takeIf { it > 0 }
                            ?: cached?.startedAtMs

                    // âœ… Playback artwork: prefer profile image (avatar). Fallback to whatever the list item had (thumbnail).
                    val avatar = getAvatarUriUnauth(streamerLogin)
                    val artwork = avatar
                        ?: incomingMeta?.artworkUri
                        ?: cached?.artwork
                        ?: Uri.EMPTY

                    val primaryUrl = when (streamSource) {
                        Constants.STREAM_SOURCE_HLS -> fetchHlsStreamUrl(streamerLogin)
                        else -> fetchProxyStreamUrl(streamerLogin, selectedProxy)
                    }

                    val finalUrl = primaryUrl ?: run {
                        when (streamSource) {
                            Constants.STREAM_SOURCE_HLS -> fetchProxyStreamUrl(streamerLogin, selectedProxy)
                            else -> fetchHlsStreamUrl(streamerLogin)
                        }
                    }

                    streamUrl = finalUrl

                    if (streamUrl != null) {
                        val mediaItem = createLiveMediaItem(
                            mediaId = mediaId,
                            title = title,
                            artist = artist,
                            artwork = artwork,
                            streamUrl = Uri.parse(streamUrl),
                            startedAtMs = startedAtMs
                        )
                        childItems.add(mediaItem)
                        if (!future.isDone) {
                            future.set(MediaSession.MediaItemsWithStartPosition(childItems, 0, 0L))
                        }
                    } else {
                        if (!future.isDone) future.setException(Exception("Failed to fetch stream URL"))
                    }
                    return@future
                }

                // Authenticated playback
                twitchUserUseCase(mediaId, null, bearer, Constants.TWITCH_CLIENT_ID).collect { userResult ->
                    if (userResult is ResponseState.Success) {
                        val user = userResult.data?.firstOrNull()
                        val artworkUriStr = user?.profileImageUrl
                        val displayName = user?.displayName ?: return@collect
                        val loginName = (user.login ?: displayName).trim().lowercase()

                        val primaryUrl = when (streamSource) {
                            Constants.STREAM_SOURCE_HLS -> fetchHlsStreamUrl(loginName)
                            else -> fetchProxyStreamUrl(loginName, selectedProxy)
                        }

                        val finalUrl = primaryUrl ?: run {
                            when (streamSource) {
                                Constants.STREAM_SOURCE_HLS -> fetchProxyStreamUrl(loginName, selectedProxy)
                                else -> fetchHlsStreamUrl(loginName)
                            }
                        }

                        streamUrl = finalUrl

                        if (streamUrl != null) {
                            twitchStreamsDetailsUseCase(mediaId, bearer, Constants.TWITCH_CLIENT_ID).collect { detailsResult ->
                                if (detailsResult is ResponseState.Success) {
                                    val details = detailsResult.data?.firstOrNull()
                                    val userName = details?.userName.orEmpty()
                                    val startedAtMs = parseTwitchIsoToMs(details?.startedAt)

                                    val artworkUri = try {
                                        artworkUriStr?.let { Uri.parse(it) } ?: Uri.EMPTY
                                    } catch (_: Exception) {
                                        Uri.EMPTY
                                    }

                                    val mediaItem = createLiveMediaItem(
                                        mediaId = mediaId,
                                        title = details?.title.orEmpty(),
                                        artist = userName,
                                        artwork = artworkUri,
                                        streamUrl = Uri.parse(streamUrl),
                                        startedAtMs = startedAtMs
                                    )
                                    childItems.add(mediaItem)

                                    if (!future.isDone) {
                                        future.set(MediaSession.MediaItemsWithStartPosition(childItems, 0, 0L))
                                    }
                                }
                            }
                        } else {
                            if (!future.isDone) future.setException(Exception("Failed to fetch stream URL"))
                        }
                    }
                }
            } catch (e: Exception) {
                if (!future.isDone) future.setException(e)
            }
        }

        return future
    }

    // -------------------- Single page loader (cursor-based pagination) --------------------

    private suspend fun loadSinglePage(
        parentId: String,
        bearer: String?,
        loggedinUserId: String?,
        cursor: String?,
        maxItems: Int
    ): Pair<List<MediaItem>, String?> {

        if (maxItems <= 0) return Pair(emptyList(), null)
        if (parentId == SEARCH) return Pair(emptyList(), null)

        // FOLLOWED streams
        if (parentId == FOLLOWED) {
            if (bearer.isNullOrBlank() || loggedinUserId.isNullOrBlank()) return Pair(emptyList(), null)

            val state = awaitNonLoading(twitchStreamsUseCase(loggedinUserId, bearer, Constants.TWITCH_CLIENT_ID, cursor))
            return when (state) {
                is ResponseState.Error -> Pair(emptyList(), null)
                is ResponseState.Success -> {
                    val (streams, nextCursor) = state.data ?: Pair(emptyList(), null)
                    val items = streams.take(maxItems).map { d ->
                        createPlayableStreamItem(
                            mediaId = d.userId,
                            title = d.title,
                            artist = d.userName,
                            artwork = Uri.parse(d.thumbnailUrl.replace("{width}", "440").replace("{height}", "248")),
                            viewerCount = d.viewerCount.toString(),
                            gameName = d.gameName
                        )
                    }
                    Pair(items, nextCursor)
                }
                else -> Pair(emptyList(), null)
            }
        }

        // TOP_STREAMS
        if (parentId == TOP_STREAMS) {
            if (bearer.isNullOrBlank()) {
                // Backend response
                val state = awaitNonLoading(getBackendTopStreamsUseCase(cursor))
                return when (state) {
                    is ResponseState.Error -> Pair(emptyList(), null)
                    is ResponseState.Success -> {
                        val (streams, nextCursor) = state.data ?: Pair(emptyList(), null)
                        val items = streams.take(maxItems).map { stream ->
                            val art = try { Uri.parse(stream.thumbnailUrl) } catch (_: Exception) { null }
                            val playbackId = stream.userLogin.takeIf { it.isNotBlank() } ?: stream.id
                            val startedAtMs = parseTwitchIsoToMs(stream.startedAt)

                            createPlayableStreamItem(
                                mediaId = playbackId,
                                title = stream.title,
                                artist = stream.userName,
                                artwork = art,
                                viewerCount = stream.viewerCount.toString(),
                                gameName = stream.gameName,
                                userLogin = stream.userLogin,
                                startedAtMs = startedAtMs
                            )
                        }
                        Pair(items, nextCursor)
                    }
                    else -> Pair(emptyList(), null)
                }
            } else {
                // Twitch API response
                val state = awaitNonLoading(getTopStreamsUseCase(bearer, Constants.TWITCH_CLIENT_ID, cursor))
                return when (state) {
                    is ResponseState.Error -> Pair(emptyList(), null)
                    is ResponseState.Success -> {
                        val (streams, nextCursor) = state.data ?: Pair(emptyList(), null)
                        val items = streams.take(maxItems).map { stream ->
                            createPlayableStreamItem(
                                mediaId = stream.userId,
                                title = stream.title,
                                artist = stream.userName,
                                artwork = Uri.parse(stream.thumbnailUrl.replace("{width}", "440").replace("{height}", "248")),
                                viewerCount = stream.viewerCount.toString(),
                                gameName = stream.gameName
                            )
                        }
                        Pair(items, nextCursor)
                    }
                    else -> Pair(emptyList(), null)
                }
            }
        }

        // TOP_GAMES
        if (parentId == TOP_GAMES) {
            if (bearer.isNullOrBlank()) {
                // Backend response
                val state = awaitNonLoading(getBackendCategoriesUseCase(cursor))
                return when (state) {
                    is ResponseState.Error -> Pair(emptyList(), null)
                    is ResponseState.Success -> {
                        val (games, nextCursor) = state.data ?: Pair(emptyList(), null)
                        val items = games.take(maxItems).map { game ->
                            val art = try { Uri.parse(game.boxArtUrl) } catch (_: Exception) { null }
                            createBrowsableItem("$GAME_PREFIX${game.id}", game.name, null, art)
                        }
                        Pair(items, nextCursor)
                    }
                    else -> Pair(emptyList(), null)
                }
            } else {
                // Twitch API response
                val state = awaitNonLoading(getTopGamesUseCase(bearer, Constants.TWITCH_CLIENT_ID, cursor))
                return when (state) {
                    is ResponseState.Error -> Pair(emptyList(), null)
                    is ResponseState.Success -> {
                        val (games, nextCursor) = state.data ?: Pair(emptyList(), null)
                        val items = games.take(maxItems).map { game ->
                            val art = try { Uri.parse(game.boxArtUrl.replace("{width}", "285").replace("{height}", "380")) } catch (_: Exception) { null }
                            createBrowsableItem("$GAME_PREFIX${game.id}", game.name, null, art)
                        }
                        Pair(items, nextCursor)
                    }
                    else -> Pair(emptyList(), null)
                }
            }
        }

        // GAME streams (game:123 format)
        if (parentId.startsWith(GAME_PREFIX)) {
            val gameId = parentId.removePrefix(GAME_PREFIX)

            if (bearer.isNullOrBlank()) {
                // Backend response
                val state = awaitNonLoading(getBackendChannelsUseCase(gameId, cursor))
                return when (state) {
                    is ResponseState.Error -> Pair(emptyList(), null)
                    is ResponseState.Success -> {
                        val (channels, nextCursor) = state.data ?: Pair(emptyList(), null)
                        val items = channels.take(maxItems).map { channel ->
                            val art = try { Uri.parse(channel.thumbnailUrl) } catch (_: Exception) { null }
                            val playbackId = channel.userLogin.takeIf { it.isNotBlank() } ?: channel.id
                            val startedAtMs = parseTwitchIsoToMs(channel.startedAt)

                            createPlayableStreamItem(
                                mediaId = playbackId,
                                title = channel.title,
                                artist = channel.userName,
                                artwork = art,
                                viewerCount = channel.viewerCount.toString(),
                                gameName = channel.gameName,
                                userLogin = channel.userLogin,
                                startedAtMs = startedAtMs
                            )
                        }
                        Pair(items, nextCursor)
                    }
                    else -> Pair(emptyList(), null)
                }
            } else {
                // Twitch API response
                val state = awaitNonLoading(getStreamsByGameUseCase(bearer, Constants.TWITCH_CLIENT_ID, gameId, cursor))
                return when (state) {
                    is ResponseState.Error -> Pair(emptyList(), null)
                    is ResponseState.Success -> {
                        val (channels, nextCursor) = state.data ?: Pair(emptyList(), null)
                        val items = channels.take(maxItems).map { channel ->
                            createPlayableStreamItem(
                                mediaId = channel.userId,
                                title = channel.title,
                                artist = channel.userName,
                                artwork = Uri.parse(channel.thumbnailUrl.replace("{width}", "440").replace("{height}", "248")),
                                viewerCount = channel.viewerCount.toString(),
                                gameName = channel.gameName
                            )
                        }
                        Pair(items, nextCursor)
                    }
                    else -> Pair(emptyList(), null)
                }
            }
        }

        // SEARCH
        if (parentId.startsWith(SEARCH_PREFIX)) {
            if (bearer.isNullOrBlank()) return Pair(emptyList(), null)

            val encoded = parentId.removePrefix(SEARCH_PREFIX)
            val query = try { URLDecoder.decode(encoded, "UTF-8") } catch (_: Exception) { encoded }

            val state = awaitNonLoading(searchTwitchChannelsUseCase(bearer, Constants.TWITCH_CLIENT_ID, query, cursor))
            return when (state) {
                is ResponseState.Error -> Pair(emptyList(), null)
                is ResponseState.Success -> {
                    val (channels, nextCursor) = state.data ?: Pair(emptyList(), null)
                    val items = channels.filter { it.isLive }.take(maxItems).map { c ->
                        val art = c.thumbnailUrl?.let { u -> try { Uri.parse(u) } catch (_: Exception) { null } }
                        createPlayableStreamItem(
                            mediaId = c.id,
                            title = c.title.orEmpty().ifBlank { "Live" },
                            artist = c.displayName,
                            artwork = art
                        )
                    }
                    Pair(items, nextCursor)
                }
                else -> Pair(emptyList(), null)
            }
        }

        return Pair(emptyList(), null)
    }

    private suspend fun <T> awaitNonLoading(flow: kotlinx.coroutines.flow.Flow<ResponseState<T>>): ResponseState<T> {
        return flow.first { it !is ResponseState.Loading }
    }

    // -------------------- Unauth profile image --------------------

    private suspend fun fetchProfileImageUnauthenticated(channelLogin: String): Uri? = withContext(Dispatchers.IO) {
        try {
            var profileImageUri: Uri? = null
            getBackendProfileImageUseCase(channelLogin.lowercase()).collect { result ->
                when (result) {
                    is ResponseState.Success -> profileImageUri = result.data?.profileImageUrl?.let { Uri.parse(it) }
                    is ResponseState.Error -> AndroidLog.w("MediaSession", "Profile image fetch error: ${result.message}")
                    is ResponseState.Loading -> {}
                }
            }
            profileImageUri
        } catch (e: Exception) {
            AndroidLog.w("MediaSession", "Could not fetch profile image for $channelLogin", e)
            null
        }
    }

    // -------------------- Stream URL fetchers --------------------

    private suspend fun fetchHlsStreamUrl(channelName: String): String? = withContext(Dispatchers.IO) {
        try {
            val normalized = channelName.trim().lowercase()
            AndroidLog.d("MediaSession", "Fetching HLS token for $normalized")

            val tokenPair = TwitchGqlService.getPlaybackAccessToken(
                normalized,
                Constants.TWITCH_GRAPHQL_CLIENT_ID
            )

            if (tokenPair == null) {
                AndroidLog.e("MediaSession", "Failed to get HLS token for $normalized")
                return@withContext null
            }

            val (token, signature) = tokenPair
            val hlsUrl = TwitchGqlService.buildHlsUrl(normalized, token, signature)
            val audioUrl = parseHlsMasterForAudio(hlsUrl)

            audioUrl ?: hlsUrl
        } catch (e: Exception) {
            AndroidLog.e("MediaSession", "Error fetching HLS URL for $channelName", e)
            null
        }
    }

    private suspend fun parseHlsMasterForAudio(masterUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(masterUrl)
                .header("User-Agent", Constants.USER_AGENT)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val entries = ExtM3UParser().parse(body)
            entries.firstOrNull { it.name == "audio_only" }?.playlistUrl
        } catch (e: Exception) {
            AndroidLog.e("MediaSession", "Error parsing HLS master", e)
            null
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun fetchProxyStreamUrl(channelLogin: String, proxyUrl: String): String? = withContext(Dispatchers.IO) {
        val safeLogin = channelLogin.trim().lowercase()
        val encodedLogin = try { URLEncoder.encode(safeLogin, "UTF-8") } catch (_: Exception) { safeLogin }

        val attempts = listOf(
            "$proxyUrl/live/$encodedLogin?allow_source=true&allow_audio_only=true&fast_bread=true",
            "$proxyUrl/live/$encodedLogin?allow_source=true&allow_audio_only=true&fast_bread=false"
        )

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        for (url in attempts) {
            try {
                val request = Request.Builder().url(url).header("User-Agent", Constants.USER_AGENT).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    AndroidLog.w("MediaSession", "Proxy request failed: ${response.code} (url=$url)")
                    continue
                }

                val body = response.body?.string() ?: continue
                val entries = ExtM3UParser().parse(body)

                val audio = entries.firstOrNull { it.name == "audio_only" }?.playlistUrl
                if (!audio.isNullOrBlank()) return@withContext audio

                val fallback = entries.firstOrNull()?.playlistUrl
                if (!fallback.isNullOrBlank()) return@withContext fallback
            } catch (e: Exception) {
                Log.e("MediaSession", "Error fetching proxy URL (url=$url)", e)
            }
        }
        null
    }

    // -------------------- MediaItem builders --------------------

    private fun createPlayableStreamItem(
        mediaId: String,
        title: String,
        artist: String,
        artwork: Uri?,
        viewerCount: String? = null,
        gameName: String? = null,
        userLogin: String? = null,
        startedAtMs: Long? = null
    ): MediaItem {
        val meta = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)

        if (artwork != null) meta.setArtworkUri(artwork)

        val extras = Bundle().apply {
            viewerCount?.let { putString("viewerCount", it) }
            gameName?.let { putString("gameName", it) }
            userLogin?.let { putString("userLogin", it) }
            if (startedAtMs != null) putLong(EXTRA_STARTED_AT_MS, startedAtMs)
        }
        meta.setExtras(extras)

        playableCache[mediaId] = CachedPlayable(
            title = title,
            artist = artist,
            artwork = artwork,
            userLogin = userLogin,
            startedAtMs = startedAtMs
        )

        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(meta.build())
            .build()
    }

    private fun createBrowsableItem(
        mediaId: String,
        title: String,
        subtitle: String? = null,
        artwork: Uri? = null
    ): MediaItem {
        val meta = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(subtitle)
            .setIsBrowsable(true)
            .setIsPlayable(false)
        if (artwork != null) meta.setArtworkUri(artwork)

        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(meta.build())
            .build()
    }

    private fun createLiveMediaItem(
        mediaId: String,
        title: String,
        artist: String,
        artwork: Uri,
        streamUrl: Uri,
        startedAtMs: Long?
    ): MediaItem {
        val uptimeString = if (startedAtMs != null) " - ${formatUptime(startedAtMs)}" else " - Live"
        val compositeArtist = "$artist$uptimeString"
        val extras = Bundle().apply {
            if (startedAtMs != null) putLong(EXTRA_STARTED_AT_MS, startedAtMs)
        }

        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(streamUrl)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(60_000)
                    .build()
            )
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(compositeArtist)
                    .setArtworkUri(artwork)
                    .setExtras(extras)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                    .build()
            )
            .build()
    }

    private val rootMediaItem: MediaItem by lazy {
        MediaItem.Builder().setMediaId(ROOT)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Streamer Audio")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()
    }

    private fun List<MediaItem>.toImmutable(): ImmutableList<MediaItem> = ImmutableList.copyOf(this)

    private fun parseTwitchIsoToMs(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(iso)?.time
        } catch (_: Exception) {
            null
        }
    }

    private fun formatUptime(startedAtMs: Long): String {
        val diff = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(0L)
        val minutes = diff / 60000
        return "${minutes / 60}h ${minutes % 60}m"
    }
}