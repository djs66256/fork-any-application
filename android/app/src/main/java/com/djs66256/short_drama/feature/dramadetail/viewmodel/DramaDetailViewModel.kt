package com.djs66256.short_drama.feature.dramadetail.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DramaDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val dramaId: String = savedStateHandle.get<String>("dramaId") ?: ""
}
