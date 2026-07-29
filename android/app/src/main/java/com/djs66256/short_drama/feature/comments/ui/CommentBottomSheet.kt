package com.djs66256.short_drama.feature.comments.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.djs66256.short_drama.domain.model.CommentSort
import com.djs66256.short_drama.feature.comments.model.CommentSource
import com.djs66256.short_drama.feature.comments.viewmodel.CommentEffect
import com.djs66256.short_drama.feature.comments.viewmodel.CommentSheetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    dramaId: String,
    source: CommentSource,
    onDismiss: () -> Unit,
    onRequireLogin: (CommentEffect.RequireLogin) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CommentSheetViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(dramaId, source) {
        viewModel.open(dramaId = dramaId, source = source)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CommentEffect.RequireLogin -> onRequireLogin(effect)
                is CommentEffect.ShowMessage -> onMessage(effect.message)
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        CommentBottomSheetContent(
            uiState = uiState,
            onSelectSort = viewModel::selectSort,
            onRetry = viewModel::retry,
            onToggleLike = viewModel::toggleLike,
            onLoadMore = viewModel::loadNextPage,
            onInputChanged = viewModel::onInputChanged,
            onSubmit = viewModel::submitComment,
            modifier = modifier,
        )
    }
}

@Composable
fun CommentBottomSheetContent(
    uiState: com.djs66256.short_drama.feature.comments.viewmodel.CommentUiState,
    onSelectSort: (CommentSort) -> Unit,
    onRetry: () -> Unit,
    onToggleLike: (String) -> Unit,
    onLoadMore: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    listModifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CommentHeader(
            totalCount = uiState.totalCount,
            selectedSort = uiState.selectedSort,
            onSelectSort = onSelectSort,
        )
        CommentListSection(
            uiState = uiState,
            onRetry = onRetry,
            onToggleLike = onToggleLike,
            onLoadMore = onLoadMore,
            modifier = listModifier.fillMaxWidth(),
        )
        CommentComposer(
            inputText = uiState.inputText,
            isSubmitting = uiState.isSubmitting,
            errorMessage = uiState.composerErrorMessage,
            onInputChanged = onInputChanged,
            onSubmit = onSubmit,
        )
    }
}
