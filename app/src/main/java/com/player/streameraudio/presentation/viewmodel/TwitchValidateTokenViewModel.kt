package com.player.streameraudio.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.player.streameraudio.data.util.ResponseState
import com.player.streameraudio.domain.usecase.GetValidTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TwitchValidateTokenViewModel @Inject constructor(
    private val validTokenUseCase: GetValidTokenUseCase
) : ViewModel(){

    private val _twitchValidTokenState = MutableStateFlow(TwitchValidateTokenState())
    val twitchValidateTokenState: StateFlow<TwitchValidateTokenState> = _twitchValidTokenState

    fun getTwitchValidateToken(
        authorization: String
    ) {

        viewModelScope.launch(Dispatchers.IO) {
            validTokenUseCase(authorization).collect {
                when(it){
                    is ResponseState.Success ->{
                        _twitchValidTokenState.value = it.data?.let { it1 -> TwitchValidateTokenState(tokenResponse = it1) }!!
                    }
                    is ResponseState.Loading ->{
                        _twitchValidTokenState.value = TwitchValidateTokenState(isLoading = true)
                    }
                    is ResponseState.Error ->{
                        _twitchValidTokenState.value = TwitchValidateTokenState(error = it.message?:"An Unexpected Error")
                    }
                }
            }
        }
    }
}
