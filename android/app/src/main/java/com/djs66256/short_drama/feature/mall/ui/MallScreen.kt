package com.djs66256.short_drama.feature.mall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.djs66256.short_drama.feature.mall.model.MallHostMessage
import com.djs66256.short_drama.feature.mall.model.MallLoginContext
import com.djs66256.short_drama.feature.mall.model.MallLoginResult
import com.djs66256.short_drama.feature.mall.viewmodel.MallContainerState
import com.djs66256.short_drama.feature.mall.viewmodel.MallEffect
import com.djs66256.short_drama.feature.mall.viewmodel.MallViewModel

@Composable
fun MallScreen(
    onOpenSearch: () -> Unit,
    onOpenMallLogin: (MallLoginContext) -> Unit,
    modifier: Modifier = Modifier,
    searchReturnSignal: Int = 0,
    loginResultSignal: Int = 0,
    latestLoginResult: MallLoginResult? = null,
    viewModel: MallViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val hostMessageDispatcher = remember { MallHostMessageDispatcher() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                MallEffect.OpenSearch -> onOpenSearch()
                is MallEffect.OpenMallLogin -> onOpenMallLogin(effect.context)
                is MallEffect.SendHostMessage -> hostMessageDispatcher.dispatch(effect.message)
            }
        }
    }

    LaunchedEffect(searchReturnSignal) {
        if (searchReturnSignal > 0) {
            viewModel.onSearchReturned()
        }
    }

    LaunchedEffect(loginResultSignal, latestLoginResult) {
        if (loginResultSignal > 0 && latestLoginResult != null) {
            viewModel.onMallLoginResult(latestLoginResult)
        }
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAppResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when (val state = uiState.state) {
            MallContainerState.Loading -> MallLoadingState()
            is MallContainerState.Error -> MallErrorState(
                message = state.message,
                onRetry = viewModel::retryLoadHome,
            )
            MallContainerState.Success -> Unit
        }

        MallWebViewContainer(
            url = uiState.currentUrl,
            modifier = Modifier.fillMaxSize(),
            onPageStateChanged = viewModel::onPageEvent,
            onBridgeMessage = viewModel::onBridgeMessage,
            hostMessageDispatcher = hostMessageDispatcher,
            isVisible = uiState.state == MallContainerState.Success,
        )
    }
}

@Composable
private fun MallLoadingState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "正在加载商城...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MallErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "商城暂时不可用",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
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

class MallHostMessageDispatcher {
    private var listener: ((MallHostMessage) -> Unit)? = null

    fun bind(listener: (MallHostMessage) -> Unit) {
        this.listener = listener
    }

    fun unbind() {
        listener = null
    }

    fun dispatch(message: MallHostMessage) {
        listener?.invoke(message)
    }
}
