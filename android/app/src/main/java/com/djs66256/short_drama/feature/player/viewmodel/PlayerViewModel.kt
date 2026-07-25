package com.djs66256.short_drama.feature.player.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.djs66256.short_drama.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val videoId: String =
        savedStateHandle.get<String>(AppDestination.Arg.VIDEO_ID)
            ?: savedStateHandle.get<String>(AppDestination.Arg.ID)
            ?: ""
}
