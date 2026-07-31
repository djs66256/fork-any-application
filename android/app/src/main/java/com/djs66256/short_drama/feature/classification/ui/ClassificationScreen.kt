package com.djs66256.short_drama.feature.classification.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.djs66256.short_drama.core.theme.ClassificationChipSurface
import com.djs66256.short_drama.core.theme.ClassificationChipText
import com.djs66256.short_drama.core.theme.ClassificationPageBackground
import com.djs66256.short_drama.core.theme.ClassificationPanelSurface
import com.djs66256.short_drama.core.theme.ClassificationPanelTitle
import com.djs66256.short_drama.core.theme.ClassificationRailBackground
import com.djs66256.short_drama.core.theme.ClassificationRailSelected
import com.djs66256.short_drama.core.theme.ClassificationRailUnselected
import com.djs66256.short_drama.core.theme.ClassificationTabSelected
import com.djs66256.short_drama.core.theme.ClassificationTabUnselected
import com.djs66256.short_drama.domain.model.ClassificationDimensionKey
import com.djs66256.short_drama.domain.model.ClassificationGender
import com.djs66256.short_drama.feature.classification.model.ClassificationDimensionUiModel
import com.djs66256.short_drama.feature.classification.viewmodel.ClassificationEffect
import com.djs66256.short_drama.feature.classification.viewmodel.ClassificationUiState
import com.djs66256.short_drama.feature.classification.viewmodel.ClassificationViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun ClassificationScreen(
    onBack: () -> Unit,
    onOpenSearchResult: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ClassificationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val latestDimensions by rememberUpdatedState(uiState.dimensions)
    val listState = rememberLazyListState()

    LaunchedEffect(viewModel, listState, latestDimensions) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ClassificationEffect.ScrollToDimension -> {
                    val targetIndex = latestDimensions.indexOfFirst { it.key == effect.key }
                    if (targetIndex >= 0) {
                        listState.animateScrollToItem(index = targetIndex)
                    }
                }
            }
        }
    }

    LaunchedEffect(listState, uiState.dimensions) {
        if (uiState.dimensions.isEmpty()) {
            return@LaunchedEffect
        }
        snapshotVisibleDimensionKey(listState, uiState.dimensions)
            .filterNotNull()
            .distinctUntilChanged()
            .collect(viewModel::onVisibleDimensionChanged)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ClassificationPageBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            ClassificationHeader(
                selected = uiState.selectedGender,
                onBack = onBack,
                onSelected = viewModel::onGenderSelected,
            )
            ClassificationContent(
                uiState = uiState,
                listState = listState,
                onRetry = viewModel::retry,
                onSelectDimension = viewModel::onDimensionSelected,
                onTagClick = { tag ->
                    viewModel.buildSearchRoute(tag)?.let(onOpenSearchResult)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ClassificationHeader(
    selected: ClassificationGender,
    onBack: () -> Unit,
    onSelected: (ClassificationGender) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 24.dp, top = 8.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "返回",
                tint = ClassificationTabSelected,
            )
        }
        Spacer(modifier = Modifier.width(18.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(30.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ClassificationGender.entries.forEach { gender ->
                val isSelected = gender == selected
                Text(
                    text = gender.label,
                    modifier = Modifier.clickable { onSelected(gender) },
                    color = if (isSelected) ClassificationTabSelected else ClassificationTabUnselected,
                    fontSize = if (isSelected) 24.sp else 22.sp,
                    lineHeight = if (isSelected) 28.sp else 26.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ClassificationContent(
    uiState: ClassificationUiState,
    listState: LazyListState,
    onRetry: () -> Unit,
    onSelectDimension: (ClassificationDimensionKey) -> Unit,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 0.dp),
    ) {
        when {
            uiState.isLoading && !uiState.hasLoadedOnce -> ClassificationLoadingState(isRefreshing = false)
            uiState.errorMessage != null && !uiState.hasLoadedOnce -> ClassificationErrorState(
                message = uiState.errorMessage.orEmpty(),
                onRetry = onRetry,
            )
            else -> ClassificationBody(
                dimensions = uiState.dimensions,
                selectedDimensionKey = uiState.selectedDimensionKey,
                listState = listState,
                onSelectDimension = onSelectDimension,
                onTagClick = onTagClick,
            )
        }

        if (uiState.isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ClassificationPageBackground.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center,
            ) {
                ClassificationLoadingState(isRefreshing = true)
            }
        }
    }
}

@Composable
private fun ClassificationBody(
    dimensions: List<ClassificationDimensionUiModel>,
    selectedDimensionKey: ClassificationDimensionKey,
    listState: LazyListState,
    onSelectDimension: (ClassificationDimensionKey) -> Unit,
    onTagClick: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        ClassificationDimensionRail(
            dimensions = dimensions,
            selectedDimensionKey = selectedDimensionKey,
            onSelectDimension = onSelectDimension,
            modifier = Modifier.width(144.dp),
        )
        ClassificationSectionList(
            dimensions = dimensions,
            listState = listState,
            onTagClick = onTagClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ClassificationDimensionRail(
    dimensions: List<ClassificationDimensionUiModel>,
    selectedDimensionKey: ClassificationDimensionKey,
    onSelectDimension: (ClassificationDimensionKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClassificationRailBackground)
            .padding(start = 22.dp, top = 28.dp, end = 8.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        dimensions.forEach { dimension ->
            val isSelected = dimension.key == selectedDimensionKey
            Text(
                text = dimension.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectDimension(dimension.key) },
                color = if (isSelected) ClassificationRailSelected else ClassificationRailUnselected,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ClassificationSectionList(
    dimensions: List<ClassificationDimensionUiModel>,
    listState: LazyListState,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 0.dp, end = 20.dp, top = 6.dp, bottom = 16.dp),
        color = ClassificationPanelSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(start = 24.dp, end = 20.dp, top = 22.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            itemsIndexed(
                items = dimensions,
                key = { _, item -> item.key.apiValue },
            ) { _, dimension ->
                ClassificationSection(
                    dimension = dimension,
                    onTagClick = onTagClick,
                )
            }
        }
    }
}

@Composable
private fun ClassificationSection(
    dimension: ClassificationDimensionUiModel,
    onTagClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = dimension.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            color = ClassificationPanelTitle,
        )
        if (dimension.tags.isEmpty()) {
            Surface(
                color = ClassificationChipSurface,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = dimension.emptyMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = ClassificationPanelTitle,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                dimension.tags.chunked(CHIP_ROW_SIZE).forEach { rowTags ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowTags.forEach { tag ->
                            ClassificationTagChip(
                                label = tag,
                                onClick = { onTagClick(tag) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(CHIP_ROW_SIZE - rowTags.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClassificationTagChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = ClassificationChipSurface,
        contentColor = ClassificationChipText,
        shape = RoundedCornerShape(12.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ClassificationLoadingState(isRefreshing: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = ClassificationRailSelected)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isRefreshing) "正在刷新分类..." else "正在加载分类...",
            style = MaterialTheme.typography.bodyLarge,
            color = ClassificationTabUnselected,
        )
    }
}

@Composable
private fun ClassificationErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "加载失败",
            style = MaterialTheme.typography.headlineMedium,
            color = ClassificationTabSelected,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = ClassificationTabUnselected,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

private fun snapshotVisibleDimensionKey(
    listState: LazyListState,
    dimensions: List<ClassificationDimensionUiModel>,
) = androidx.compose.runtime.snapshotFlow {
    listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
        ?.let(dimensions::getOrNull)
        ?.key
}

private const val CHIP_ROW_SIZE = 3
