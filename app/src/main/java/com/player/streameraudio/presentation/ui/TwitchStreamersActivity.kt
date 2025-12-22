package com.player.streameraudio.presentation.ui

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.common.util.concurrent.ListenableFuture
import com.player.streameraudio.R
import com.player.streameraudio.data.util.Constants
import com.player.streameraudio.data.util.SecurePreferences
import com.player.streameraudio.presentation.adapter.StreamersAdaptor
import com.player.streameraudio.presentation.viewmodel.TwitchStreamersListViewModel
import com.player.streameraudio.presentation.viewmodel.TwitchUserViewModel
import com.player.streameraudio.service.PlaybackService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TwitchStreamersActivity : AppCompatActivity(), RecyclerViewInterface {

    private lateinit var browserFuture: ListenableFuture<MediaBrowser>
    private val browser: MediaBrowser?
        get() = if (::browserFuture.isInitialized && browserFuture.isDone && !browserFuture.isCancelled) {
            browserFuture.get()
        } else null

    private var subItemMediaList: MutableList<MediaItem> = mutableListOf()

    private val viewModel: TwitchStreamersListViewModel by viewModels()
    private val userViewModel: TwitchUserViewModel by viewModels()
    private lateinit var adapter: StreamersAdaptor
    private var accessToken: String? = null
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    // Pagination
    private val currentPageByParent = mutableMapOf<String, Int>()
    private val isLoadingMoreByParent = mutableMapOf<String, Boolean>()
    private val hasMoreByParent = mutableMapOf<String, Boolean>()

    // Browse navigation
    private var currentParentId: String = SECTION_FOLLOWED
    private val navStack: ArrayDeque<Pair<String, String>> = ArrayDeque() // parentId -> title
    private var hasInitializedView = false // Track if we've loaded the initial view

    // Mini player views
    private lateinit var miniPlayerBar: View
    private lateinit var miniArtwork: ImageView
    private lateinit var miniTitle: TextView
    private lateinit var miniSubtitle: TextView
    private lateinit var miniPlayPause: ImageView

    // Use encrypted SharedPreferences
    private val securePrefs: SharedPreferences by lazy {
        SecurePreferences.getEncryptedSharedPreferences(this)
    }

    private val miniPlayerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            runOnUiThread { updateMiniPlayer() }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            runOnUiThread { updateMiniPlayer() }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            runOnUiThread { updateMiniPlayer() }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_twitch_streamers)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        accessToken = intent.getStringExtra("access_token")
            ?: securePrefs.getString(Constants.PREF_ACCESS_TOKEN, null)

        // Restore navigation state if available
        savedInstanceState?.let { bundle ->
            currentParentId = bundle.getString(KEY_CURRENT_PARENT_ID, currentParentId)
            title = bundle.getString(KEY_CURRENT_TITLE, title?.toString())

            // Restore navigation stack
            val stackSize = bundle.getInt(KEY_NAV_STACK_SIZE, 0)
            navStack.clear()
            for (i in 0 until stackSize) {
                val parentId = bundle.getString("${KEY_NAV_STACK_PARENT}_$i")
                val stackTitle = bundle.getString("${KEY_NAV_STACK_TITLE}_$i")
                if (parentId != null && stackTitle != null) {
                    navStack.addLast(parentId to stackTitle)
                }
            }

            hasInitializedView = true // Mark as already initialized
        }

        // Set initial title based on authentication (only if not restoring state)
        if (!hasInitializedView) {
            title = if (accessToken.isNullOrBlank()) "Categories" else "Following"
            currentParentId = if (accessToken.isNullOrBlank()) SECTION_TOP_GAMES else SECTION_FOLLOWED
        }

        // SwipeRefresh
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.purple_500),
            ContextCompat.getColor(this, R.color.purple_700)
        )
        swipeRefreshLayout.setOnRefreshListener { refreshStreams() }

        // Recycler
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        adapter = StreamersAdaptor(recyclerView.context, emptyList(), this)
        recyclerView.adapter = adapter
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        // Pagination scroll listener
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // Only trigger when scrolling down
                if (dy <= 0) return

                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                val isLoading = isLoadingMoreByParent[currentParentId] ?: false
                val hasMore = hasMoreByParent[currentParentId] ?: true

                // Load more when near bottom (10 items before end)
                val threshold = 10
                if (!isLoading && hasMore) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - threshold) {
                        loadMoreItems()
                    }
                }
            }
        })

        // Mini player views
        miniPlayerBar = findViewById(R.id.miniPlayerBar)
        miniArtwork = findViewById(R.id.miniArtwork)
        miniTitle = findViewById(R.id.miniTitle)
        miniSubtitle = findViewById(R.id.miniSubtitle)
        miniPlayPause = findViewById(R.id.miniPlayPause)

        // Mini player click -> open Details
        miniPlayerBar.setOnClickListener { openNowPlayingDetails() }

        // Play/pause button
        miniPlayPause.setOnClickListener {
            val b = browser ?: return@setOnClickListener
            if (b.isPlaying) b.pause() else b.play()
        }

        // Back navigation for browse tree (Top Games -> streams, etc.)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (navStack.isNotEmpty()) {
                    val (parentId, parentTitle) = navStack.removeLast()
                    loadParent(parentId, parentTitle, addToStack = false)
                } else {
                    finish()
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save current navigation state
        outState.putString(KEY_CURRENT_PARENT_ID, currentParentId)
        outState.putString(KEY_CURRENT_TITLE, title?.toString())

        // Save navigation stack
        outState.putInt(KEY_NAV_STACK_SIZE, navStack.size)
        navStack.forEachIndexed { index, (parentId, stackTitle) ->
            outState.putString("${KEY_NAV_STACK_PARENT}_$index", parentId)
            outState.putString("${KEY_NAV_STACK_TITLE}_$index", stackTitle)
        }
    }

    override fun onItemClick(position: Int) {
        val selectedItem = adapter.getItemAtPosition(position)

        // Browse folders (Followed/Top Games/Top Streams/Game, etc.)
        if (selectedItem.mediaMetadata.isBrowsable == true) {
            val nextTitle = selectedItem.mediaMetadata.title?.toString().orEmpty().ifBlank { "Browse" }
            loadParent(selectedItem.mediaId, nextTitle, addToStack = true)
            return
        }

        // Playable stream -> open details and autoplay
        val rawArtist = selectedItem.mediaMetadata.artist?.toString().orEmpty()
        val cleanName = if (rawArtist.contains(" - ")) rawArtist.substringBefore(" - ") else rawArtist

        val intent = Intent(this@TwitchStreamersActivity, TwitchStreamersDetailsActivity::class.java)
        intent.putExtra("streamerId", selectedItem.mediaId)
        intent.putExtra("streamerName", cleanName) // username
        intent.putExtra("streamerTitle", selectedItem.mediaMetadata.title?.toString()) // stream title
        intent.putExtra("access_token", accessToken)
        intent.putExtra("forcePlay", true) // Ã¢Å“â€¦ ensures autoplay from list
        startActivity(intent)
    }

    override fun onStart() {
        super.onStart()
        initializeBrowser()
    }

    override fun onStop() {
        releaseBrowser()
        super.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        // Add sections based on authentication status
        if (!accessToken.isNullOrBlank()) {
            menu.add(0, MENU_FOLLOWED, 0, "Following")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }

        menu.add(0, MENU_TOP_GAMES, 1, "Categories")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(0, MENU_TOP_STREAMS, 2, "Live")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        // Add Search only when authenticated
        if (!accessToken.isNullOrBlank()) {
            val searchItem = menu.add(0, MENU_SEARCH, 3, "Search")
            searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
            val searchView = SearchView(this)
            searchView.queryHint = "Search live channels"
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    val q = query?.trim().orEmpty()
                    if (q.isNotBlank()) {
                        performSearch(q)
                        searchView.clearFocus()
                        searchItem.collapseActionView()
                    }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean = false
            })
            searchItem.actionView = searchView
        }

        // Add Login menu item when not authenticated
        if (accessToken.isNullOrBlank()) {
            menu.add(0, MENU_LOGIN, 4, "Login")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }

        return true
    }

    @OptIn(UnstableApi::class)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (navStack.isNotEmpty()) {
                    val (parentId, parentTitle) = navStack.removeLast()
                    loadParent(parentId, parentTitle, addToStack = false)
                    true
                } else {
                    super.onOptionsItemSelected(item)
                }
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            MENU_FOLLOWED -> {
                if (accessToken.isNullOrBlank()) {
                    Toast.makeText(this, "Please login to view followed channels", Toast.LENGTH_SHORT).show()
                    return true
                }
                navStack.clear()
                loadParent(SECTION_FOLLOWED, "Following", addToStack = false)
                true
            }
            MENU_TOP_GAMES -> {
                navStack.clear()
                loadParent(SECTION_TOP_GAMES, "Categories", addToStack = false)
                true
            }
            MENU_TOP_STREAMS -> {
                navStack.clear()
                loadParent(SECTION_TOP_STREAMS, "Live", addToStack = false)
                true
            }
            MENU_LOGIN -> {
                // Redirect to MainActivity for login
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initializeBrowser() {
        browserFuture =
            MediaBrowser.Builder(
                this,
                SessionToken(this, ComponentName(this, PlaybackService::class.java))
            ).buildAsync()

        browserFuture.addListener({
            val b = browser ?: return@addListener
            b.addListener(miniPlayerListener)

            // Only load initial section on first initialization
            if (!hasInitializedView) {
                val initialSection = if (accessToken.isNullOrBlank()) SECTION_TOP_GAMES else SECTION_FOLLOWED
                val initialTitle = if (accessToken.isNullOrBlank()) "Categories" else "Following"
                currentParentId = initialSection
                title = initialTitle
                hasInitializedView = true
                displayChildrenById(currentParentId, page = 0, clearList = true)
            } else {
                // Re-connecting browser, restore current view
                supportActionBar?.setDisplayHomeAsUpEnabled(navStack.isNotEmpty())
                displayChildrenById(currentParentId, page = 0, clearList = true)
            }

            runOnUiThread { updateMiniPlayer() }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun loadParent(parentId: String, newTitle: String, addToStack: Boolean) {
        if (addToStack) {
            navStack.addLast(currentParentId to (title?.toString().orEmpty()))
        }
        currentParentId = parentId
        title = newTitle
        supportActionBar?.setDisplayHomeAsUpEnabled(navStack.isNotEmpty())

        // Reset pagination state for new parent
        currentPageByParent[parentId] = 0
        isLoadingMoreByParent[parentId] = false
        hasMoreByParent[parentId] = true
        subItemMediaList.clear()

        swipeRefreshLayout.isRefreshing = true
        displayChildrenById(parentId, page = 0, clearList = true)
    }

    private fun displayChildrenById(parentId: String, page: Int = 0, clearList: Boolean = false) {
        val b = browser ?: return

        val pageSize = 50
        android.util.Log.d("TwitchStreamers", "displayChildrenById: $parentId, page=$page, clear=$clearList")

        // Request 50 items per page to match backend/Twitch API response size
        val childrenFuture = b.getChildren(parentId, page, pageSize, null)

        if (clearList) {
            subItemMediaList.clear()
        }

        childrenFuture.addListener({
            val result = childrenFuture.get()
            val children = result.value ?: emptyList()

            android.util.Log.d("TwitchStreamers", "Got ${children.size} items for page $page of $parentId")

            // Check for duplicates before adding
            val existingIds = subItemMediaList.map { it.mediaId }.toSet()
            val newItems = children.filter { it.mediaId !in existingIds }

            android.util.Log.d("TwitchStreamers", "Filtered: ${newItems.size} new items (${children.size - newItems.size} duplicates)")

            // Only append truly new items
            subItemMediaList.addAll(newItems)
            adapter.updateData(subItemMediaList)

            // Update pagination state
            isLoadingMoreByParent[parentId] = false

            // IMPROVED: Stop only when backend returns less than full page
            // This means we've reached the end of available data
            val hasMore = children.size >= pageSize
            hasMoreByParent[parentId] = hasMore

            android.util.Log.d("TwitchStreamers", "Total items: ${subItemMediaList.size}, hasMore=$hasMore (backend returned ${children.size})")

            swipeRefreshLayout.isRefreshing = false
        }, ContextCompat.getMainExecutor(this))
    }

    private fun refreshStreams() {
        // Reset pagination and reload from page 0
        currentPageByParent[currentParentId] = 0
        isLoadingMoreByParent[currentParentId] = false
        hasMoreByParent[currentParentId] = true
        displayChildrenById(currentParentId, page = 0, clearList = true)
    }

    private fun loadMoreItems() {
        val parentId = currentParentId
        val currentPage = currentPageByParent[parentId] ?: 0
        val nextPage = currentPage + 1

        android.util.Log.d("TwitchStreamers", "Loading more items for $parentId: page $nextPage")

        // Mark as loading
        isLoadingMoreByParent[parentId] = true
        currentPageByParent[parentId] = nextPage

        // Load next page (don't clear list)
        displayChildrenById(parentId, page = nextPage, clearList = false)
    }

    private fun performSearch(query: String) {
        val encoded = try {
            java.net.URLEncoder.encode(query, "UTF-8")
        } catch (_: Exception) {
            query
        }
        navStack.clear()
        loadParent("search:$encoded", "Search: $query", addToStack = false)
    }

    private fun releaseBrowser() {
        try {
            browser?.removeListener(miniPlayerListener)
        } catch (_: Exception) { }
        if (::browserFuture.isInitialized) {
            MediaBrowser.releaseFuture(browserFuture)
        }
    }

    // -------------------------
    // Mini player helpers
    // -------------------------

    private fun updateMiniPlayer() {
        val b = browser
        val item = b?.currentMediaItem

        if (b == null || item == null) {
            miniPlayerBar.visibility = View.GONE
            return
        }

        miniPlayerBar.visibility = View.VISIBLE

        val meta = item.mediaMetadata

        // Your service sometimes sets artist like "Name - 1h 20m"
        val rawArtist = meta.artist?.toString().orEmpty()
        val cleanName = if (rawArtist.contains(" - ")) rawArtist.substringBefore(" - ") else rawArtist

        miniTitle.text = cleanName.ifBlank { "Now Playing" }
        miniSubtitle.text = meta.title?.toString().orEmpty()

        meta.artworkUri?.let { uri ->
            Glide.with(this).load(uri).into(miniArtwork)
        }

        // Use built-in icons (no extra drawables needed)
        if (b.isPlaying) {
            miniPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        } else {
            miniPlayPause.setImageResource(android.R.drawable.ic_media_play)
        }
    }

    private fun openNowPlayingDetails() {
        val b = browser ?: return
        val item = b.currentMediaItem ?: return

        val meta = item.mediaMetadata
        val rawArtist = meta.artist?.toString().orEmpty()
        val cleanName = if (rawArtist.contains(" - ")) rawArtist.substringBefore(" - ") else rawArtist

        val intent = Intent(this, TwitchStreamersDetailsActivity::class.java)
        intent.putExtra("streamerId", item.mediaId)
        intent.putExtra("streamerName", cleanName)
        intent.putExtra("streamerTitle", meta.title?.toString().orEmpty())
        intent.putExtra("access_token", accessToken)
        startActivity(intent)
    }

    companion object {
        // These IDs must match CustomMediaLibrarySessionCallback nodes.
        private const val SECTION_FOLLOWED = "FOLLOWED"
        private const val SECTION_TOP_GAMES = "TOP_GAMES"
        private const val SECTION_TOP_STREAMS = "TOP_STREAMS"

        private const val MENU_FOLLOWED = 1001
        private const val MENU_TOP_GAMES = 1002
        private const val MENU_TOP_STREAMS = 1003
        private const val MENU_SEARCH = 1004
        private const val MENU_LOGIN = 1005

        // Saved state keys
        private const val KEY_CURRENT_PARENT_ID = "current_parent_id"
        private const val KEY_CURRENT_TITLE = "current_title"
        private const val KEY_NAV_STACK_SIZE = "nav_stack_size"
        private const val KEY_NAV_STACK_PARENT = "nav_stack_parent"
        private const val KEY_NAV_STACK_TITLE = "nav_stack_title"
    }
}