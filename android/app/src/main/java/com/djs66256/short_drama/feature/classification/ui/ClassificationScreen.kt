package com.djs66256.short_drama.feature.classification.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.djs66256.short_drama.domain.model.ClassificationDimensionKey
import com.djs66256.short_drama.domain.model.ClassificationGender
import kotlinx.coroutines.flow.collect
import com.djs66256.short_drama.feature.classification.model.ClassificationDimensionUiModel
import com.djs66256.short_drama.feature.classification.viewmodel.ClassificationEffect
import com.djs66256.short_drama.feature.classification.viewmodel.ClassificationUiState
import com.djs66256.short_drama.feature.classification.viewmodel.ClassificationViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

@OptIn(ExperimentalMaterial3Api::class)
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

    LaunchedEffect(viewModel, listState) {
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("分类") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            ClassificationGenderTabs(
                selected = uiState.selectedGender,
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
private fun ClassificationGenderTabs(
    selected: ClassificationGender,
    onSelected: (ClassificationGender) -> Unit,
) {
    TabRow(selectedTabIndex = ClassificationGender.entries.indexOf(selected)) {
        ClassificationGender.entries.forEach { gender ->
            Tab(
                selected = gender == selected,
                onClick = { onSelected(gender) },
                text = { Text(gender.label) },
            )
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
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
            modifier = Modifier.width(92.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
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
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        dimensions.forEach { dimension ->
            val isSelected = dimension.key == selectedDimensionKey
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onSelectDimension(dimension.key) },
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = dimension.title,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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

@Composable
private fun ClassificationSection(
    dimension: ClassificationDimensionUiModel,
    onTagClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = dimension.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (dimension.tags.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = dimension.emptyMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dimension.tags.chunked(CHIP_ROW_SIZE).forEach { rowTags ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(20.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ClassificationLoadingState(isRefreshing: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isRefreshing) "正在刷新分类..." else "正在加载分类...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ClassificationErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "加载失败",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
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
