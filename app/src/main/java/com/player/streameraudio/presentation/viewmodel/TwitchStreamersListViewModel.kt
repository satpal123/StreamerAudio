package com.player.streameraudio.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.player.streameraudio.data.util.ResponseState
import com.player.streameraudio.domain.usecase.GetTwitchStreamsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TwitchStreamersListViewModel @Inject constructor(
    private val twitchStreamsUseCase: GetTwitchStreamsUseCase
) :ViewModel() {

    private val _twitchStreamsState = MutableStateFlow(TwitchStreamersState())
    val twitchStreamsState: StateFlow<TwitchStreamersState> = _twitchStreamsState

    fun getTwitchStreams(
        userId: String?,
        authorization: String,
        clientId: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            twitchStreamsUseCase(userId, authorization, clientId).collect {
                when(it){
                    is ResponseState.Success ->{
                        // Extract the list from the Pair (first element)
                        val (streamsList, _) = it.data ?: Pair(emptyList(), null)
                        _twitchStreamsState.value = TwitchStreamersState(streamersList = streamsList)
                    }
                    is ResponseState.Loading ->{
                        _twitchStreamsState.value = TwitchStreamersState(isLoading = true)
                    }
                    is ResponseState.Error ->{
                        _twitchStreamsState.value = TwitchStreamersState(error = it.message?:"An Unexpected Error")
                    }
                }
            }
        }
    }
}