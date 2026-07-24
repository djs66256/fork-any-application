package com.djs66256.short_drama.feature.player.viewmodel

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerViewModelTest {

    @Test
    fun `T-08 videoId extracted from SavedStateHandle`() {
        val savedStateHandle = SavedStateHandle(mapOf("videoId" to "001"))
        val viewModel = PlayerViewModel(savedStateHandle)
        assertEquals("001", viewModel.videoId)
    }

    @Test
    fun `T-08 videoId defaults to empty string when not in SavedStateHandle`() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = PlayerViewModel(savedStateHandle)
        assertEquals("", viewModel.videoId)
    }
}
