package com.djs66256.short_drama.feature.comments.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.djs66256.short_drama.domain.model.CommentSort
import com.djs66256.short_drama.feature.comments.model.CommentSource
import com.djs66256.short_drama.feature.comments.viewmodel.CommentEffect
import com.djs66256.short_drama.feature.comments.viewmodel.CommentSheetViewModel
import com.djs66256.short_drama.feature.comments.viewmodel.CommentUiState

private val CommentSheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = CommentSheetShape,
        dragHandle = null,
        containerColor = CommentSheetSurface,
        tonalElevation = 0.dp,
    ) {
        CommentBottomSheetContent(
            uiState = uiState,
            onDismiss = onDismiss,
            onSelectSort = viewModel::selectSort,
            onRetry = viewModel::retry,
            onToggleLike = viewModel::toggleLike,
            onLoadMore = viewModel::loadNextPage,
            onInputChanged = viewModel::onInputChanged,
            onSubmit = viewModel::submitComment,
        )
    }
}

@Composable
fun CommentBottomSheetContent(
    uiState: CommentUiState,
    onDismiss: () -> Unit,
    onSelectSort: (CommentSort) -> Unit,
    onRetry: () -> Unit,
    onToggleLike: (String) -> Unit,
    onLoadMore: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    listModifier: Modifier = Modifier,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 460.dp, max = screenHeight * 0.68f)
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CommentHeader(
            totalCount = uiState.totalCount,
            selectedSort = uiState.selectedSort,
            onDismiss = onDismiss,
            onSelectSort = onSelectSort,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp, max = screenHeight * 0.32f),
        ) {
            CommentListSection(
                uiState = uiState,
                onRetry = onRetry,
                onToggleLike = onToggleLike,
                onLoadMore = onLoadMore,
                modifier = listModifier.fillMaxWidth(),
            )
        }
        CommentComposer(
            inputText = uiState.inputText,
            isSubmitting = uiState.isSubmitting,
            errorMessage = uiState.composerErrorMessage,
            onInputChanged = onInputChanged,
            onSubmit = onSubmit,
        )
    }
}
