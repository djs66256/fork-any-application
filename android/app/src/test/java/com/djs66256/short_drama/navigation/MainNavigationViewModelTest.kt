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
    fun `T-01 search pending routes are published`() = runTest {
        val viewModel = MainNavigationViewModel()

        viewModel.uiState.test {
            assertEquals(null, awaitItem().pendingRoute)
            viewModel.enqueuePendingRoute(PendingRoute.SearchResult("逆袭"))
            assertEquals(PendingRoute.SearchResult("逆袭"), awaitItem().pendingRoute)
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

    @Test
    fun `T-01 menu state transitions from closed to opening to open`() = runTest {
        val viewModel = MainNavigationViewModel()

        viewModel.uiState.test {
            assertEquals(MenuPanelPresentationState.CLOSED, awaitItem().menuPanelState)

            viewModel.openMenu()
            assertEquals(MenuPanelPresentationState.OPENING, awaitItem().menuPanelState)

            viewModel.onMenuOpened()
            assertEquals(MenuPanelPresentationState.OPEN, awaitItem().menuPanelState)
        }
    }

    @Test
    fun `T-01 closeMenuThenNavigate closes first and publishes route after animation finished`() = runTest {
        val viewModel = MainNavigationViewModel()

        viewModel.uiState.test {
            assertEquals(MenuPanelPresentationState.CLOSED, awaitItem().menuPanelState)

            viewModel.openMenu()
            assertEquals(MenuPanelPresentationState.OPENING, awaitItem().menuPanelState)

            viewModel.onMenuOpened()
            assertEquals(MenuPanelPresentationState.OPEN, awaitItem().menuPanelState)

            viewModel.closeMenuThenNavigate(PendingRoute.MenuLogin)
            val closingState = awaitItem()
            assertEquals(MenuPanelPresentationState.CLOSING, closingState.menuPanelState)
            assertEquals(PendingRoute.MenuLogin, closingState.pendingMenuRoute)
            assertEquals(null, closingState.pendingRoute)

            viewModel.onMenuClosedAnimationFinished()
            val closedState = awaitItem()
            assertEquals(MenuPanelPresentationState.CLOSED, closedState.menuPanelState)
            assertEquals(null, closedState.pendingMenuRoute)
            assertEquals(PendingRoute.MenuLogin, closedState.pendingRoute)
        }
    }

    @Test
    fun `T-01 closing ignores later menu navigation requests and keeps first target`() = runTest {
        val viewModel = MainNavigationViewModel()

        viewModel.uiState.test {
            assertEquals(MenuPanelPresentationState.CLOSED, awaitItem().menuPanelState)

            viewModel.openMenu()
            assertEquals(MenuPanelPresentationState.OPENING, awaitItem().menuPanelState)

            viewModel.onMenuOpened()
            assertEquals(MenuPanelPresentationState.OPEN, awaitItem().menuPanelState)

            viewModel.closeMenuThenNavigate(PendingRoute.MenuLogin)
            val firstClosingState = awaitItem()
            assertEquals(MenuPanelPresentationState.CLOSING, firstClosingState.menuPanelState)
            assertEquals(PendingRoute.MenuLogin, firstClosingState.pendingMenuRoute)

            viewModel.closeMenuThenNavigate(PendingRoute.MenuMessages)
            expectNoEvents()

            viewModel.openMenu()
            expectNoEvents()

            viewModel.onMenuClosedAnimationFinished()
            val finalState = awaitItem()
            assertEquals(MenuPanelPresentationState.CLOSED, finalState.menuPanelState)
            assertEquals(PendingRoute.MenuLogin, finalState.pendingRoute)
            assertEquals(null, finalState.pendingMenuRoute)
        }
    }

    @Test
    fun `T-01 closeMenu without target only returns to closed state`() = runTest {
        val viewModel = MainNavigationViewModel()

        viewModel.uiState.test {
            assertEquals(MenuPanelPresentationState.CLOSED, awaitItem().menuPanelState)

            viewModel.openMenu()
            assertEquals(MenuPanelPresentationState.OPENING, awaitItem().menuPanelState)

            viewModel.onMenuOpened()
            assertEquals(MenuPanelPresentationState.OPEN, awaitItem().menuPanelState)

            viewModel.closeMenu()
            val closingState = awaitItem()
            assertEquals(MenuPanelPresentationState.CLOSING, closingState.menuPanelState)
            assertEquals(null, closingState.pendingMenuRoute)

            viewModel.onMenuClosedAnimationFinished()
            val finalState = awaitItem()
            assertEquals(MenuPanelPresentationState.CLOSED, finalState.menuPanelState)
            assertEquals(null, finalState.pendingMenuRoute)
            assertEquals(null, finalState.pendingRoute)
        }
    }
}
