package com.player.streameraudio.presentation.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.player.streameraudio.data.settings.PlaybackMode
import com.player.streameraudio.data.settings.ProxyEndpoint
import com.player.streameraudio.presentation.viewmodel.MainUiState
import com.player.streameraudio.presentation.viewmodel.MainViewModel
import com.player.streameraudio.service.CustomMediaLibrarySessionCallback
import kotlinx.coroutines.delay
import kotlin.math.abs

private data class Destination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
private val destinations = listOf(
    Destination("following", "Following", Icons.Default.Favorite),
    Destination("browse", "Browse", Icons.Default.Category),
    Destination("live", "Live", Icons.Default.LiveTv),
    Destination("library", "Library", Icons.Default.LibraryMusic)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamerAudioApp(viewModel: MainViewModel, onLogin: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "browse"
    val selectedDestination = if (route == "category" || route == "search") "browse" else route
    val showMainNavigation = route == "category" || route == "search" || route in destinations.map { it.route }
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val snackbar = remember { SnackbarHostState() }
    val favoriteKeys = state.settings.favoriteStreams.map { it.stableKey }.toSet()
    val playAndOpen: (MediaItem) -> Unit = { item ->
        viewModel.play(item)
        navController.navigate("player") { launchSingleTop = true }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }
    LaunchedEffect(route, state.authenticated) {
        when (route) {
            "browse" -> viewModel.loadSection(CustomMediaLibrarySessionCallback.TOP_GAMES, "Browse")
            "live" -> viewModel.loadSection(CustomMediaLibrarySessionCallback.TOP_STREAMS, "Live")
            "library" -> if (state.currentParentId == CustomMediaLibrarySessionCallback.RECENTS) {
                viewModel.loadSection(CustomMediaLibrarySessionCallback.RECENTS, "Recently played")
            } else {
                viewModel.loadSection(CustomMediaLibrarySessionCallback.FAVORITES, "Favorites")
            }
            "following" -> if (state.authenticated) {
                viewModel.loadSection(CustomMediaLibrarySessionCallback.FOLLOWED, "Following")
            }
        }
    }
    if (state.settings.legacyProxyNoticePending) {
        AlertDialog(
            onDismissRequest = viewModel::consumeLegacyNotice,
            title = { Text("Proxy settings changed") },
            text = { Text("Built-in proxy servers were removed. Direct playback is now the default; add your own compatible HTTPS endpoint in Settings if needed.") },
            confirmButton = { TextButton(onClick = { viewModel.consumeLegacyNotice(); navController.navigate("settings") }) { Text("Open settings") } },
            dismissButton = { TextButton(onClick = viewModel::consumeLegacyNotice) { Text("Not now") } }
        )
    }

    Box(Modifier.fillMaxSize()) {
        AtmosphericBackground()
        Row(Modifier.fillMaxSize()) {
            if (wide && showMainNavigation) {
                AppNavigationRail(selectedDestination, navController)
            }
            Scaffold(
            modifier = Modifier.weight(1f),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            snackbarHost = { SnackbarHost(snackbar) },
            floatingActionButton = {
                if (route != "player" && showMainNavigation) {
                    state.playback.mediaItem?.let {
                        MiniPlayer(state, viewModel::togglePlayback) { navController.navigate("player") }
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.Center,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            if (route in destinations.map { it.route }) {
                                Text(
                                    "STREAMER AUDIO",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                when (route) {
                                    "following" -> "Following"
                                    "browse" -> "Browse"
                                    "live" -> "Live"
                                    "library" -> "Library"
                                    "search" -> "Search"
                                    "profile" -> "Profile"
                                    "settings" -> "Settings"
                                    "player" -> "Now playing"
                                    else -> state.title
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        if (route == "settings" || route == "player" || route == "category" || route == "search" || route == "profile") {
                            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                        }
                    },
                    actions = {
                        if (route in destinations.map { it.route } || route == "category") {
                            IconButton(onClick = { navController.navigate("search") }) { Icon(Icons.Default.Search, "Search") }
                        }
                        if (state.authenticated && route !in listOf("profile", "player")) {
                            IconButton(onClick = { navController.navigate("profile") }) {
                                if (state.profileImageUrl != null) {
                                    AsyncImage(
                                        model = state.profileImageUrl,
                                        contentDescription = "Profile for ${state.userLogin ?: "Twitch user"}",
                                        modifier = Modifier.size(32.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.AccountCircle, "Profile for ${state.userLogin ?: "Twitch user"}")
                                }
                            }
                        }
                        if (route !in listOf("settings", "player")) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                IconButton(onClick = { navController.navigate("settings") }) { Icon(Icons.Default.Settings, "Settings") }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)
                    )
                )
            },
            bottomBar = {
                if (!wide && showMainNavigation) AppNavigationBar(selectedDestination, navController)
            }
        ) { padding ->
            NavHost(
                navController,
                startDestination = "browse",
                modifier = Modifier
                    .padding(padding)
                    .pointerInput(route) {
                        if (route in destinations.map { it.route }) {
                            var dragDistance = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { dragDistance = 0f },
                                onHorizontalDrag = { change, amount ->
                                    dragDistance += amount
                                    change.consume()
                                },
                                onDragEnd = {
                                    if (abs(dragDistance) >= 72.dp.toPx()) {
                                        val currentIndex = destinations.indexOfFirst { it.route == route }
                                        val nextIndex = if (dragDistance < 0f) currentIndex + 1 else currentIndex - 1
                                        destinations.getOrNull(nextIndex)?.let { destination ->
                                            navController.navigate(destination.route) {
                                                popUpTo("browse")
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    },
                enterTransition = {
                    val initialIndex = destinations.indexOfFirst { it.route == initialState.destination.route }
                    val targetIndex = destinations.indexOfFirst { it.route == targetState.destination.route }
                    val forward = initialIndex < 0 || targetIndex < 0 || targetIndex > initialIndex
                    slideInHorizontally(animationSpec = tween(280)) { width -> if (forward) width else -width }
                },
                exitTransition = {
                    val initialIndex = destinations.indexOfFirst { it.route == initialState.destination.route }
                    val targetIndex = destinations.indexOfFirst { it.route == targetState.destination.route }
                    val forward = initialIndex < 0 || targetIndex < 0 || targetIndex > initialIndex
                    slideOutHorizontally(animationSpec = tween(280)) { width -> if (forward) -width else width }
                },
                popEnterTransition = {
                    slideInHorizontally(animationSpec = tween(280)) { width -> -width }
                },
                popExitTransition = {
                    slideOutHorizontally(animationSpec = tween(280)) { width -> width }
                }
            ) {
                composable("following") {
                    if (state.authenticated) ContentScreen(state, onRetry = { viewModel.loadSection(CustomMediaLibrarySessionCallback.FOLLOWED, "Following") }, onLoadMore = viewModel::loadNextPage, favoriteKeys = favoriteKeys, onToggleFavorite = viewModel::toggleFavorite, onItem = playAndOpen)
                    else LoginPrompt(onLogin)
                }
                composable("browse") {
                    ContentScreen(state, grid = true, onRetry = { viewModel.loadSection(CustomMediaLibrarySessionCallback.TOP_GAMES, "Browse") }, onLoadMore = viewModel::loadNextPage) { item ->
                        viewModel.loadSection(item.mediaId, item.mediaMetadata.title?.toString() ?: "Category")
                        navController.navigate("category")
                    }
                }
                composable("live") {
                    ContentScreen(state, onRetry = { viewModel.loadSection(CustomMediaLibrarySessionCallback.TOP_STREAMS, "Live") }, onLoadMore = viewModel::loadNextPage, favoriteKeys = favoriteKeys, onToggleFavorite = viewModel::toggleFavorite, onItem = playAndOpen)
                }
                composable("category") { ContentScreen(state, onRetry = viewModel::reloadCurrentSection, onLoadMore = viewModel::loadNextPage, favoriteKeys = favoriteKeys, onToggleFavorite = viewModel::toggleFavorite, onItem = playAndOpen) }
                composable("search") {
                    if (state.authenticated) SearchScreen(state, viewModel, favoriteKeys, playAndOpen)
                    else LoginPrompt(onLogin)
                }
                composable("library") {
                    LibraryScreen(state, viewModel, favoriteKeys, playAndOpen)
                }
                composable("player") { PlayerScreen(state, viewModel) }
                composable("profile") {
                    if (state.authenticated) ProfileScreen(state, viewModel::logout)
                    else LoginPrompt(onLogin)
                }
                composable("settings") { SettingsScreen(state, viewModel, onLogin) }
            }
        }
        }
        if (route == "player" && state.playback.isResolving) {
            PlaybackLoadingOverlay(state)
        }
    }
}

@Composable
private fun PlaybackLoadingOverlay(state: MainUiState) {
    val interactionSource = remember { MutableInteractionSource() }
    val channel = state.playback.mediaItem?.mediaMetadata?.artist?.toString().orEmpty()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(20f)
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 36.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(Modifier.size(48.dp), strokeWidth = 4.dp)
                Text("Connecting to stream", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (channel.isNotBlank()) {
                    Text(channel, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun AtmosphericBackground() {
    val surface = MaterialTheme.colorScheme.surface
    val primaryTint = lerp(surface, MaterialTheme.colorScheme.primaryContainer, 0.16f)
    val accentTint = lerp(surface, MaterialTheme.colorScheme.tertiary, 0.06f)
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        surface,
                        primaryTint,
                        accentTint,
                        surface
                    )
                )
            )
    )
}

@Composable
private fun AppNavigationBar(route: String, nav: NavHostController) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f),
        tonalElevation = 6.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(62.dp)
        ) {
            destinations.forEach { destination ->
                val selected = route == destination.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = {
                                nav.navigate(destination.route) {
                                    popUpTo("browse")
                                    launchSingleTop = true
                                }
                            }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.width(48.dp).height(28.dp),
                        shape = CircleShape,
                        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                destination.icon,
                                destination.label,
                                modifier = Modifier.size(22.dp),
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        destination.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun AppNavigationRail(route: String, nav: NavHostController) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f)
    ) {
        Spacer(Modifier.height(72.dp))
        destinations.forEach { destination ->
            NavigationRailItem(
                selected = route == destination.route,
                onClick = { nav.navigate(destination.route) { launchSingleTop = true } },
                icon = { Icon(destination.icon, destination.label) }, label = { Text(destination.label) }
            )
        }
    }
}

@Composable
private fun SearchScreen(
    state: MainUiState,
    viewModel: MainViewModel,
    favoriteKeys: Set<String>,
    onItem: (MediaItem) -> Unit
) {
    var query by rememberSaveable { mutableStateOf(state.searchQuery.orEmpty()) }
    var hasSearched by rememberSaveable { mutableStateOf(state.searchQuery != null) }
    val focusManager = LocalFocusManager.current
    val submitSearch: () -> Unit = {
        if (query.trim().length >= 2) {
            hasSearched = true
            viewModel.searchChannels(query)
            focusManager.clearFocus()
        } else {
            viewModel.searchChannels(query)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
            tonalElevation = 4.dp
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                label = { Text("Search live channels") },
                placeholder = { Text("Channel name") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    FilledTonalIconButton(onClick = submitSearch, enabled = !state.loading) {
                        Icon(Icons.Default.Search, "Search")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitSearch() })
            )
        }
        if (!hasSearched) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f)) {
                        Icon(Icons.Default.Search, null, Modifier.padding(18.dp).size(40.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Text("Find a live channel", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Search Twitch channels that are live now.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Box(Modifier.weight(1f)) {
                ContentScreen(
                    state = state,
                    onRetry = submitSearch,
                    onLoadMore = {},
                    favoriteKeys = favoriteKeys,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onItem = onItem
                )
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    state: MainUiState,
    viewModel: MainViewModel,
    favoriteKeys: Set<String>,
    onItem: (MediaItem) -> Unit
) {
    val showingFavorites = state.currentParentId != CustomMediaLibrarySessionCallback.RECENTS
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = showingFavorites,
                onClick = {
                    viewModel.loadSection(CustomMediaLibrarySessionCallback.FAVORITES, "Favorites")
                },
                label = { Text("Favorites") },
                leadingIcon = { Icon(Icons.Default.Favorite, null, Modifier.size(18.dp)) }
            )
            FilterChip(
                selected = !showingFavorites,
                onClick = {
                    viewModel.loadSection(CustomMediaLibrarySessionCallback.RECENTS, "Recently played")
                },
                label = { Text("Recent") },
                leadingIcon = { Icon(Icons.Default.History, null, Modifier.size(18.dp)) }
            )
            Spacer(Modifier.weight(1f))
            if (!showingFavorites && state.settings.recentStreams.isNotEmpty()) {
                TextButton(onClick = viewModel::clearRecents) { Text("Clear") }
            }
        }
        Box(Modifier.weight(1f)) {
            ContentScreen(
                state = state,
                onRetry = viewModel::reloadCurrentSection,
                onLoadMore = viewModel::loadNextPage,
                favoriteKeys = favoriteKeys,
                onToggleFavorite = viewModel::toggleFavorite,
                emptyMessage = if (showingFavorites) {
                    "Save channels with the heart button to find them here."
                } else {
                    "Channels you play will appear here."
                },
                onItem = onItem
            )
        }
    }
}

@Composable
private fun LoginPrompt(onLogin: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Default.GraphicEq, null, Modifier.padding(18.dp).size(42.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Text("Your live follows, in audio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Connect Twitch to hear channels you follow. You can still explore Browse and Live as a guest.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Filled.Login, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Connect Twitch")
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(state: MainUiState, onLogout: () -> Unit) {
    val login = state.userLogin.orEmpty().ifBlank { "Twitch user" }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier.size(104.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 6.dp
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.profileImageUrl != null) {
                    AsyncImage(
                        model = state.profileImageUrl,
                        contentDescription = "$login profile picture",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        login.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Text(login, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
            Text(
                "Twitch connected",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f))
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Your account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Signed in as $login", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Authentication is stored securely on this device and is used to load channels you follow.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Disconnect Twitch")
        }
        Text(
            "Streamer Audio is independent and is not affiliated with Twitch.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ContentScreen(
    state: MainUiState,
    grid: Boolean = false,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    favoriteKeys: Set<String> = emptySet(),
    onToggleFavorite: ((MediaItem) -> Unit)? = null,
    emptyMessage: String? = null,
    onItem: (MediaItem) -> Unit
) {
    val widthDp = LocalConfiguration.current.screenWidthDp
    val categoryColumns = when {
        widthDp >= 840 -> 6
        widthDp >= 600 -> 4
        else -> 3
    }
    when {
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.errorMessage != null -> ErrorState(state.errorMessage, onRetry)
        state.items.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                emptyMessage ?: if (state.title == "Search") "No live channels matched your search." else "Nothing live here right now.",
                style = MaterialTheme.typography.titleMedium
            )
        }
        grid -> CategoryGrid(state, categoryColumns, onLoadMore, onItem)
        else -> StreamList(state, onLoadMore, favoriteKeys, onToggleFavorite, onItem)
    }
}

@Composable
private fun CategoryGrid(
    state: MainUiState,
    columns: Int,
    onLoadMore: () -> Unit,
    onItem: (MediaItem) -> Unit
) {
    val gridState = rememberLazyGridState()
    LaunchedEffect(gridState, state.items.size, state.canLoadMore, state.loadingMore) {
        snapshotFlow {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= gridState.layoutInfo.totalItemsCount - (columns * 2)
        }.collect { nearEnd ->
            if (nearEnd && state.canLoadMore && !state.loadingMore) onLoadMore()
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        contentPadding = PaddingValues(
            start = 14.dp,
            top = 14.dp,
            end = 14.dp,
            bottom = if (state.playback.mediaItem != null) 128.dp else 14.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(
            items = state.items,
            key = { index, item -> "${item.mediaId}:$index" }
        ) { _, item ->
            CategoryCard(item) { onItem(item) }
        }
        if (state.loadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) { LoadingNextPage() }
        }
    }
}

@Composable
private fun StreamList(
    state: MainUiState,
    onLoadMore: () -> Unit,
    favoriteKeys: Set<String>,
    onToggleFavorite: ((MediaItem) -> Unit)?,
    onItem: (MediaItem) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState, state.items.size, state.canLoadMore, state.loadingMore) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= listState.layoutInfo.totalItemsCount - 3
        }.collect { nearEnd ->
            if (nearEnd && state.canLoadMore && !state.loadingMore) onLoadMore()
        }
    }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = if (state.playback.mediaItem != null) 128.dp else 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = state.items,
            key = { index, item -> "${item.mediaId}:$index" }
        ) { _, item ->
            StreamCard(
                item = item,
                isFavorite = item.stableStreamKey() in favoriteKeys,
                onToggleFavorite = onToggleFavorite?.let { toggle -> { toggle(item) } },
                onClick = { onItem(item) }
            )
        }
        if (state.loadingMore) item { LoadingNextPage() }
    }
}

@Composable
private fun LoadingNextPage() {
    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(30.dp), strokeWidth = 3.dp)
    }
}

@Composable
private fun CategoryCard(item: MediaItem, onClick: () -> Unit) {
    val viewers = item.mediaMetadata.extras?.getString("viewerCount")
    val tags = item.mediaMetadata.extras?.getStringArrayList("tags").orEmpty()
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().aspectRatio(0.74f),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.mediaMetadata.artworkUri,
                contentDescription = item.mediaMetadata.title?.toString(),
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0x33000000), Color(0xF2000000))
                    )
                )
            )
            tags.firstOrNull()?.let { tag ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.94f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(7.dp)
                ) {
                    Text(tag, Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
            Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                Text(
                    item.mediaMetadata.title?.toString().orEmpty(),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!viewers.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Visibility, null, Modifier.size(12.dp), tint = Color.White.copy(alpha = 0.82f))
                        Spacer(Modifier.width(4.dp))
                        Text("${formatViewerCount(viewers)} watching", color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamCard(
    item: MediaItem,
    isFavorite: Boolean,
    onToggleFavorite: (() -> Unit)?,
    onClick: () -> Unit
) {
    val extras = item.mediaMetadata.extras
    val viewers = extras?.getString("viewerCount")
    val game = extras?.getString("gameName")
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.76f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                AsyncImage(
                    model = item.mediaMetadata.artworkUri,
                    contentDescription = null,
                    modifier = Modifier.width(112.dp).height(82.dp).clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                ) {
                    Text("LIVE", Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onError, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.padding(start = 14.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.mediaMetadata.artist?.toString().orEmpty().ifBlank { "Live channel" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.mediaMetadata.title?.toString().orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!viewers.isNullOrBlank()) {
                        Icon(Icons.Default.Visibility, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(formatViewerCount(viewers), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!viewers.isNullOrBlank() && !game.isNullOrBlank()) Text(" • ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!game.isNullOrBlank()) Text(game, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (onToggleFavorite != null) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(44.dp)) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.PlayArrow, "Play")
            }
        }
    }
}

private fun MediaItem.stableStreamKey(): String =
    mediaMetadata.extras?.getString("userLogin")
        ?.trim()
        ?.lowercase()
        ?.takeIf { it.isNotBlank() }
        ?: mediaId.trim().lowercase()

private fun formatViewerCount(raw: String): String {
    val value = raw.toLongOrNull() ?: return raw
    return when {
        value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
        value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
        else -> value.toString()
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.ErrorOutline, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
            Text(message)
            OutlinedButton(onClick = onRetry) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Try again") }
        }
    }
}

@Composable
private fun MiniPlayer(state: MainUiState, toggle: () -> Unit, open: () -> Unit) {
    val item = state.playback.mediaItem ?: return
    val sleepRemaining = rememberSleepTimerRemaining(state.settings.sleepTimerDeadlineEpochMs)
    Surface(
        onClick = open,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
        tonalElevation = 6.dp,
        shadowElevation = 10.dp
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(item.mediaMetadata.artworkUri, null, Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    sleepRemaining?.let { "SLEEP · ${formatTimerRemaining(it)}" } ?: "NOW PLAYING",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(item.mediaMetadata.artist?.toString().orEmpty(), maxLines = 1, fontWeight = FontWeight.Bold, overflow = TextOverflow.Ellipsis)
                Text(item.mediaMetadata.title?.toString().orEmpty(), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f))
            }
            FilledIconButton(onClick = toggle, modifier = Modifier.size(52.dp)) {
                Icon(if (state.playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (state.playback.isPlaying) "Pause" else "Play")
            }
        }
    }
}

@Composable
private fun PlayerScreen(state: MainUiState, viewModel: MainViewModel) {
    val item = state.playback.mediaItem
    if (item == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Choose a live stream to start listening.") }
        return
    }
    val sleepRemaining = rememberSleepTimerRemaining(state.settings.sleepTimerDeadlineEpochMs)
    val isFavorite = item.stableStreamKey() in state.settings.favoriteStreams.map { it.stableKey }.toSet()
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(8.dp))
        AsyncImage(item.mediaMetadata.artworkUri, null, Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Crop)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.mediaMetadata.artist?.toString().orEmpty(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = { viewModel.toggleFavorite(item) }) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(item.mediaMetadata.title?.toString().orEmpty(), maxLines = 2, overflow = TextOverflow.Ellipsis)
        state.playback.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error); OutlinedButton(onClick = viewModel::retryPlayback) { Text("Reconnect") } }
        FilledIconButton(onClick = viewModel::togglePlayback, modifier = Modifier.size(72.dp)) {
            Icon(if (state.playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (state.playback.isPlaying) "Pause" else "Play", Modifier.size(36.dp))
        }
        Text(
            sleepRemaining?.let { "Sleep timer · ${formatTimerRemaining(it)}" } ?: "Sleep timer",
            style = MaterialTheme.typography.titleSmall,
            color = if (sleepRemaining != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15, 30, 45, 60).forEach { minutes ->
                FilterChip(
                    selected = sleepRemaining != null && state.selectedSleepTimerMinutes == minutes,
                    onClick = { viewModel.setSleepTimer(minutes) },
                    label = { Text("$minutes m") }
                )
            }
            FilterChip(sleepRemaining == null, { viewModel.setSleepTimer(null) }, { Text("Off") })
        }
    }
}

@Composable
private fun rememberSleepTimerRemaining(deadlineEpochMs: Long?): Long? {
    var nowEpochMs by remember(deadlineEpochMs) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(deadlineEpochMs) {
        while (deadlineEpochMs != null && nowEpochMs < deadlineEpochMs) {
            delay(1_000L)
            nowEpochMs = System.currentTimeMillis()
        }
    }
    return deadlineEpochMs
        ?.minus(nowEpochMs)
        ?.coerceAtLeast(0L)
        ?.takeIf { it > 0L }
}

private fun formatTimerRemaining(remainingMs: Long): String {
    val totalSeconds = (remainingMs + 999L) / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Composable
private fun SettingsScreen(state: MainUiState, viewModel: MainViewModel, onLogin: () -> Unit) {
    var editing by remember { mutableStateOf<ProxyEndpoint?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    val context = LocalContext.current
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            SettingsSectionTitle("Playback")
            Card(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Audio route", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Direct is recommended. Choose a custom proxy only when you need an alternate route.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(state.settings.routing.mode == PlaybackMode.DIRECT, { viewModel.setMode(PlaybackMode.DIRECT) }, { Text("Direct") })
                        FilterChip(state.settings.routing.mode == PlaybackMode.PROXY, { viewModel.setMode(PlaybackMode.PROXY) }, { Text("Custom proxy") })
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Volume leveling", fontWeight = FontWeight.Medium)
                            Text(
                                "Boost quieter streams and gently compress loud peaks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.settings.volumeLevelingEnabled,
                            onCheckedChange = viewModel::setVolumeLeveling
                        )
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Automatic fallback", fontWeight = FontWeight.Medium)
                            Text("Try the alternate route if playback fails", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(state.settings.routing.fallbackEnabled, viewModel::setFallback, enabled = state.settings.routing.selectedProxyId != null)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SettingsSectionTitle("Proxy endpoints", Modifier.weight(1f))
                Button(onClick = { editing = null; showEditor = true }) { Icon(Icons.Default.Add, null); Text("Add") }
            }
            Text("Optional HTTPS endpoints using the /live/{channel} contract.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.settings.proxies.isEmpty()) item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Direct playback is active", fontWeight = FontWeight.Bold)
                    Text("No proxy is needed unless direct playback is unavailable.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(state.settings.proxies, key = { it.id }) { proxy ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(state.settings.routing.selectedProxyId == proxy.id, { viewModel.selectProxy(proxy.id) })
                    Column(Modifier.weight(1f)) { Text(proxy.name, fontWeight = FontWeight.Bold); Text(proxy.baseUrl, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    IconButton(onClick = { editing = proxy; showEditor = true }) { Icon(Icons.Default.Edit, "Edit ${proxy.name}") }
                    IconButton(onClick = { viewModel.deleteProxy(proxy.id) }) { Icon(Icons.Default.Delete, "Delete ${proxy.name}") }
                }
            }
        }
        item {
            SettingsSectionTitle("Account")
            Card(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Icon(Icons.Default.Favorite, null, Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(if (state.authenticated) "Connected to Twitch" else "Guest mode", fontWeight = FontWeight.Bold)
                        Text(if (state.authenticated) state.userLogin ?: "Twitch user" else "Connect to see followed live channels", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (state.authenticated) TextButton(onClick = viewModel::logout) { Text("Disconnect") }
                    else Button(onClick = onLogin) { Text("Connect") }
                }
            }
        }
        item {
            SettingsSectionTitle("About")
            Card(Modifier.fillMaxWidth().padding(top = 10.dp), shape = RoundedCornerShape(22.dp)) {
                ListItem(
                    headlineContent = { Text("Privacy policy") },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://streameraudio-twitch-backend.netlify.app/privacy"))) }
                        .semantics { contentDescription = "Open privacy policy" }
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("Support") },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://streameraudio-twitch-backend.netlify.app/support"))) }
                        .semantics { contentDescription = "Open support" }
                )
            }
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.Top) {
                Text("Independent app • Not affiliated with or endorsed by Twitch.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (showEditor) ProxyEditorDialog(editing, onDismiss = { showEditor = false }) { name, url, result ->
        val callback: (Result<ProxyEndpoint>) -> Unit = { value -> result(value.exceptionOrNull()?.message); if (value.isSuccess) showEditor = false }
        editing?.let { viewModel.updateProxy(it.id, name, url, callback) } ?: viewModel.addProxy(name, url, callback)
    }
}

@Composable
private fun SettingsSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun ProxyEditorDialog(proxy: ProxyEndpoint?, onDismiss: () -> Unit, save: (String, String, (String?) -> Unit) -> Unit) {
    var name by remember(proxy) { mutableStateOf(proxy?.name.orEmpty()) }
    var url by remember(proxy) { mutableStateOf(proxy?.baseUrl.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (proxy == null) "Add proxy" else "Edit proxy") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(url, { url = it }, label = { Text("HTTPS base URL") }, singleLine = true, isError = error != null, supportingText = { error?.let { Text(it) } })
            }
        },
        confirmButton = { TextButton(onClick = { save(name, url) { error = it } }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
