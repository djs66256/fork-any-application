package com.djs66256.short_drama.feature.ranking.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.djs66256.short_drama.R

internal enum class RankingShowcaseMode {
    HOT,
    BOOKING,
    ;

    companion object {
        const val EXTRA = "ranking_screenshot_mode"

        fun fromRaw(rawValue: String?): RankingShowcaseMode? = when (rawValue?.trim()?.lowercase()) {
            "hot" -> HOT
            "booking" -> BOOKING
            else -> null
        }
    }
}

internal fun resolveRankingShowcaseMode(
    rawMode: String?,
    isDebug: Boolean,
): RankingShowcaseMode? {
    return if (isDebug) {
        RankingShowcaseMode.fromRaw(rawMode)
    } else {
        null
    }
}

@Composable
internal fun RankingShowcaseScreen(
    mode: RankingShowcaseMode,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = mode.drawableRes()),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
    }
}

@DrawableRes
private fun RankingShowcaseMode.drawableRes(): Int = when (this) {
    RankingShowcaseMode.HOT -> R.drawable.ranking_showcase_hot
    RankingShowcaseMode.BOOKING -> R.drawable.ranking_showcase_booking
}
