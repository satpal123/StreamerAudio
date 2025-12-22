package com.player.streameraudio.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.player.streameraudio.data.util.ResponseState
import com.player.streameraudio.domain.usecase.GetTwitchUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TwitchUserViewModel @Inject constructor(
    private val twitchUserUseCase: GetTwitchUserUseCase
) : ViewModel(){

    private val _twitchUserState = MutableStateFlow(TwitchUserState())
    val twitchUserState: StateFlow<TwitchUserState> = _twitchUserState

    fun getTwitchUser(
        userId: String?,
        login:String?,
        authorization: String,
        clientId: String
    ) {

        viewModelScope.launch(Dispatchers.IO) {
            twitchUserUseCase(userId,login, authorization, clientId).collect {
                when(it){
                    is ResponseState.Success ->{
                        _twitchUserState.value = TwitchUserState(userList = it.data?: emptyList())
                    }
                    is ResponseState.Loading ->{
                        _twitchUserState.value = TwitchUserState(isLoading = true)
                    }
                    is ResponseState.Error ->{
                        _twitchUserState.value = TwitchUserState(error = it.message?:"An Unexpected Error")
                    }
                }
            }
        }
    }
}
