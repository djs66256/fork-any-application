package com.djs66256.short_drama.feature.theater.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.djs66256.short_drama.feature.theater.viewmodel.TheaterEffect
import com.djs66256.short_drama.feature.theater.viewmodel.TheaterViewModel

@Composable
fun TheaterScreen(
    onNavigateToRoute: (String) -> Unit,
    onOpenPlay: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TheaterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TheaterEffect.Navigate -> onNavigateToRoute(effect.route)
                is TheaterEffect.OpenPlay -> onOpenPlay(effect.videoId)
                is TheaterEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    TheaterContent(
        uiState = uiState,
        onSearchClick = viewModel::onSearchClick,
        onScanClick = viewModel::onScanClick,
        onChannelSelected = viewModel::onChannelSelected,
        onShortcutClick = viewModel::onShortcutClick,
        onRetry = viewModel::retry,
        onLoadNextPage = viewModel::loadNextPageIfNeeded,
        onDramaClick = viewModel::onDramaClick,
        modifier = modifier,
    )
}
