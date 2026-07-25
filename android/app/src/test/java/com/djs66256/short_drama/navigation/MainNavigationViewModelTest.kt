package com.djs66256.short_drama.navigation

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MainNavigationViewModelTest {

    @Test
    fun `enqueuePendingRoute publishes pending route`() = runTest {
        val viewModel = MainNavigationViewModel()

        viewModel.uiState.test {
            assertEquals(null, awaitItem().pendingRoute)
            viewModel.enqueuePendingRoute(PendingRoute.Play("123"))
            assertEquals(PendingRoute.Play("123"), awaitItem().pendingRoute)
        }
    }

    @Test
    fun `consumePendingRoute clears pending route`() = runTest {
        val viewModel = MainNavigationViewModel()
        viewModel.enqueuePendingRoute(PendingRoute.Play("123"))

        viewModel.uiState.test {
            assertEquals(PendingRoute.Play("123"), awaitItem().pendingRoute)
            viewModel.consumePendingRoute()
            assertEquals(null, awaitItem().pendingRoute)
        }
    }

    @Test
    fun `rejectPendingRoute clears pending route and stores reason`() = runTest {
        val viewModel = MainNavigationViewModel()
        viewModel.enqueuePendingRoute(PendingRoute.Play("123"))

        viewModel.uiState.test {
            assertEquals(PendingRoute.Play("123"), awaitItem().pendingRoute)
            viewModel.rejectPendingRoute(NavigationErrorCode.INVALID_ROUTE_PARAMS)
            val state = awaitItem()
            assertEquals(null, state.pendingRoute)
            assertEquals(NavigationErrorCode.INVALID_ROUTE_PARAMS, state.lastRejectedReason)
        }
    }
}
