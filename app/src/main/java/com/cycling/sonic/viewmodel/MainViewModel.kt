package com.cycling.sonic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycling.data.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val playerManager: PlayerManager
) : ViewModel() {

    init {
        viewModelScope.launch {
            playerManager.connect()
        }
    }
}
