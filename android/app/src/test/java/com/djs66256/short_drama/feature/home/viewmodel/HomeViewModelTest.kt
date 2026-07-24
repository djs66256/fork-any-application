package com.djs66256.short_drama.feature.home.viewmodel

import app.cash.turbine.test
import com.djs66256.short_drama.core.config.AppConfig
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `T-06 uiState transitions from loading to populated`() = runTest {
        val mockConfig = mockk<AppConfig> {
            every { appName } returns "ShortDrama"
            every { appVersion } returns "0.1.0"
        }
        val viewModel = HomeViewModel(mockConfig)

        viewModel.uiState.test {
            // First emission: isLoading=true
            val firstState = awaitItem()
            assertTrue("First state should have isLoading=true", firstState.isLoading)

            // Advance to process the viewModelScope.launch
            advanceUntilIdle()

            // Second emission: isLoading=false, fields populated
            val secondState = awaitItem()
            assertFalse("Second state should have isLoading=false", secondState.isLoading)
            assertEquals("ShortDrama", secondState.appName)
            assertEquals("0.1.0", secondState.appVersion)
        }
    }
}
