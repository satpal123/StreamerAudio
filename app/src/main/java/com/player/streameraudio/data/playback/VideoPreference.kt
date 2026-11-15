package com.player.streameraudio.data.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory choice between audio-only and video playback, plus the maximum video height.
 * It is intentionally not persisted: every app launch starts in audio-only mode so that
 * Android Auto and background listening never download video by accident.
 */
@Singleton
class VideoPreference @Inject constructor() {
    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    /** Maximum video height in pixels, or null for automatic quality. */
    private val _maxHeight = MutableStateFlow<Int?>(null)
    val maxHeight: StateFlow<Int?> = _maxHeight.asStateFlow()

    fun setEnabled(value: Boolean) { _enabled.value = value }
    fun setMaxHeight(value: Int?) { _maxHeight.value = value }
}
