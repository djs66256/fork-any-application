package com.djs66256.short_drama.feature.menu.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.djs66256.short_drama.navigation.MenuPanelPresentationState

private const val DRAWER_WIDTH_RATIO = 0.78f
private const val DRAWER_ANIMATION_DURATION_MS = 240

@Composable
fun MenuPanelDrawer(
    menuState: MenuPanelPresentationState,
    onClose: () -> Unit,
    onOpened: () -> Unit,
    onClosedAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shouldBeOpen = menuState == MenuPanelPresentationState.OPENING ||
        menuState == MenuPanelPresentationState.OPEN
    val progress by animateFloatAsState(
        targetValue = if (shouldBeOpen) 1f else 0f,
        animationSpec = tween(
            durationMillis = DRAWER_ANIMATION_DURATION_MS,
            easing = FastOutSlowInEasing,
        ),
        label = "menuPanelProgress",
        finishedListener = { value ->
            if (value == 1f && menuState == MenuPanelPresentationState.OPENING) {
                onOpened()
            }
            if (value == 0f && menuState == MenuPanelPresentationState.CLOSING) {
                onClosedAnimationFinished()
            }
        },
    )

    BackHandler(enabled = shouldHandleMenuBack(menuState)) {
        onClose()
    }

    if (!shouldRenderMenuDrawer(menuState, progress)) {
        return
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val drawerWidth = maxWidth * DRAWER_WIDTH_RATIO
        val density = LocalDensity.current
        val offsetXPx = with(density) { (-drawerWidth * (1f - progress)).toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f * progress))
                .alpha(progress)
                .clickable(onClick = onClose),
        )

        Surface(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(drawerWidth)
                .graphicsLayer {
                    translationX = offsetXPx
                },
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 12.dp,
        ) {
            content()
        }
    }
}

internal fun shouldHandleMenuBack(menuState: MenuPanelPresentationState): Boolean {
    return menuState != MenuPanelPresentationState.CLOSED
}

internal fun shouldRenderMenuDrawer(
    menuState: MenuPanelPresentationState,
    progress: Float,
): Boolean {
    return menuState != MenuPanelPresentationState.CLOSED || progress > 0f
}
