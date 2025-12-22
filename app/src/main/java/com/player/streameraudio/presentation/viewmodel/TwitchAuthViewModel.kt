package com.player.streameraudio.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.player.streameraudio.data.datasource.dto.TwitchAccessTokenDTO
import com.player.streameraudio.data.util.ResponseState
import com.player.streameraudio.domain.usecase.AuthorizeTwitchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TwitchAuthViewModel  @Inject constructor(): ViewModel() {

    private val _responseState = MutableStateFlow<ResponseState<TwitchAccessTokenDTO>>(ResponseState.Loading())
    val responseState: Flow<ResponseState<TwitchAccessTokenDTO>> = _responseState

    fun handleOAuthResponse(params: Map<String, String>) {
        viewModelScope.launch(Dispatchers.IO) {
            AuthorizeTwitchUseCase.handleOAuthResponse(params)
                .onEach { state ->
                    _responseState.value = state
                }
                .catch { e ->
                    _responseState.value = ResponseState.Error("Failed to handle OAuth response: ${e.message}")
                }
                .collect()
        }
    }
}