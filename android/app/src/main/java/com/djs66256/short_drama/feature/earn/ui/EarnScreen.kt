package com.djs66256.short_drama.feature.earn.ui

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
import androidx.compose.runtime.DisposableEffect
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
import com.djs66256.short_drama.feature.earn.model.EarnHostMessage
import com.djs66256.short_drama.feature.earn.model.EarnLoginContext
import com.djs66256.short_drama.feature.earn.model.EarnLoginResult
import com.djs66256.short_drama.feature.earn.model.EarnTaskContext
import com.djs66256.short_drama.feature.earn.model.EarnTaskPlayerResult
import com.djs66256.short_drama.feature.earn.viewmodel.EarnContainerState
import com.djs66256.short_drama.feature.earn.viewmodel.EarnEffect
import com.djs66256.short_drama.feature.earn.viewmodel.EarnViewModel

@Composable
fun EarnScreen(
    onOpenEarnLogin: (EarnLoginContext) -> Unit,
    onOpenEarnTaskPlayer: (EarnTaskContext) -> Unit,
    modifier: Modifier = Modifier,
    loginResultSignal: Int = 0,
    latestLoginResult: EarnLoginResult? = null,
    taskPlayerResultSignal: Int = 0,
    latestTaskPlayerResult: EarnTaskPlayerResult? = null,
    viewModel: EarnViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val hostMessageDispatcher = remember { EarnHostMessageDispatcher() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is EarnEffect.OpenEarnLogin -> onOpenEarnLogin(effect.context)
                is EarnEffect.OpenEarnTaskPlayer -> onOpenEarnTaskPlayer(effect.context)
                is EarnEffect.SendHostMessage -> hostMessageDispatcher.dispatch(effect.message)
            }
        }
    }

    LaunchedEffect(loginResultSignal, latestLoginResult) {
        if (loginResultSignal > 0 && latestLoginResult != null) {
            viewModel.onEarnLoginResult(latestLoginResult)
        }
    }

    LaunchedEffect(taskPlayerResultSignal, latestTaskPlayerResult) {
        if (taskPlayerResultSignal > 0 && latestTaskPlayerResult != null) {
            viewModel.onEarnTaskPlayerResult(latestTaskPlayerResult)
        }
    }

    DisposableEffect(lifecycleOwner) {
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
            EarnContainerState.Loading -> EarnLoadingState()
            is EarnContainerState.Error -> EarnErrorState(
                message = state.message,
                onRetry = viewModel::retryLoadHome,
            )
            EarnContainerState.Success -> Unit
        }

        EarnWebViewContainer(
            url = uiState.currentUrl,
            modifier = Modifier.fillMaxSize(),
            onPageStateChanged = viewModel::onPageEvent,
            onBridgeMessage = viewModel::onBridgeMessage,
            hostMessageDispatcher = hostMessageDispatcher,
            isVisible = uiState.state == EarnContainerState.Success,
        )
    }
}

@Composable
private fun EarnLoadingState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "正在加载赚钱页...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EarnErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "赚钱页暂时不可用",
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

class EarnHostMessageDispatcher {
    private var listener: ((EarnHostMessage) -> Unit)? = null

    fun bind(listener: (EarnHostMessage) -> Unit) {
        this.listener = listener
    }

    fun unbind() {
        listener = null
    }

    fun dispatch(message: EarnHostMessage) {
        listener?.invoke(message)
    }
}
