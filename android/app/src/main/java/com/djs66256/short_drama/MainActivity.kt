package com.djs66256.short_drama

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.djs66256.short_drama.core.auth.AuthBootstrapper
import com.djs66256.short_drama.core.theme.ShortDramaTheme
import com.djs66256.short_drama.domain.model.ClassificationGender
import com.djs66256.short_drama.domain.model.RankingContentType
import com.djs66256.short_drama.domain.model.RankingType
import com.djs66256.short_drama.feature.classification.ui.CLASSIFICATION_UI_VERIFICATION_SCREEN
import com.djs66256.short_drama.feature.classification.ui.ClassificationVerificationScreen
import com.djs66256.short_drama.feature.comments.ui.COMMENT_UI_VERIFICATION_SCREEN
import com.djs66256.short_drama.feature.comments.ui.CommentUiVerificationScreen
import com.djs66256.short_drama.feature.ranking.model.RankingDramaItemUiModel
import com.djs66256.short_drama.feature.ranking.model.RankingDetailTagTone
import com.djs66256.short_drama.feature.ranking.model.RankingDetailTagUiModel
import com.djs66256.short_drama.feature.ranking.model.RankingMetricVisual
import com.djs66256.short_drama.feature.ranking.model.RankingPosterStyle
import com.djs66256.short_drama.feature.ranking.ui.RankingScreenContent
import com.djs66256.short_drama.feature.ranking.viewmodel.RankingUiState
import com.djs66256.short_drama.navigation.DeeplinkRouteParser
import com.djs66256.short_drama.navigation.MainNavigationViewModel
import com.djs66256.short_drama.navigation.NavGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val navigationViewModel: MainNavigationViewModel by viewModels()
    private var debugScreenOverride by mutableStateOf<String?>(null)

    @Inject
    lateinit var authBootstrapper: AuthBootstrapper

    private var rankingShowcaseMode by mutableStateOf<RankingShowcaseMode?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rankingShowcaseMode = resolveRankingShowcaseMode(intent)
        handleDeepLink(intent)
        enableEdgeToEdge()
        setContent {
            ShortDramaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val showcaseMode = rankingShowcaseMode
                    when {
                        showcaseMode != null -> {
                            RankingScreenContent(
                                uiState = rankingShowcaseState(showcaseMode),
                                onBack = ::finish,
                                onContentTypeSelected = {},
                                onRankingTypeSelected = {},
                                onRetry = {},
                                onRetryAppend = {},
                                onLoadNextPage = {},
                                onOpenPlay = {},
                                onBook = {},
                            )
                        }

                        debugScreenOverride == COMMENT_UI_VERIFICATION_SCREEN -> {
                            CommentUiVerificationScreen()
                        }

                        debugScreenOverride?.startsWith(CLASSIFICATION_UI_VERIFICATION_SCREEN) == true -> {
                            ClassificationVerificationScreen(
                                initialGender = resolveClassificationVerificationGender(debugScreenOverride),
                            )
                        }

                        else -> {
                            val navController = rememberNavController()
                            NavGraph(
                                navController = navController,
                                navigationViewModel = navigationViewModel,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            authBootstrapper.restoreIfNeeded()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        rankingShowcaseMode = resolveRankingShowcaseMode(intent)
        handleDeepLink(intent)
    }

    fun handleDeepLink(intent: Intent) {
        val debugScreen = intent.getStringExtra(DEBUG_SCREEN_KEY)
        debugScreenOverride = debugScreen?.takeIf {
            it == COMMENT_UI_VERIFICATION_SCREEN || it.startsWith(CLASSIFICATION_UI_VERIFICATION_SCREEN)
        }
        if (debugScreenOverride != null) {
            return
        }
        val route = DeeplinkRouteParser.parse(intent.data) ?: return
        navigationViewModel.enqueuePendingRoute(route)
    }

    private companion object {
        const val DEBUG_SCREEN_KEY = "debug_screen"
    }
}

internal enum class RankingShowcaseMode {
    HOT,
    RECOMMEND,
    BOOKING,
}

private fun resolveRankingShowcaseMode(intent: Intent): RankingShowcaseMode? {
    val rawMode = intent.getStringExtra("ranking_showcase_mode")
        ?: intent.data?.getQueryParameter("showcase")
    return parseRankingShowcaseMode(rawMode)
}

internal fun parseRankingShowcaseMode(rawMode: String?): RankingShowcaseMode? = when (rawMode?.trim()?.lowercase()) {
    "hot" -> RankingShowcaseMode.HOT
    "recommend" -> RankingShowcaseMode.RECOMMEND
    "booking" -> RankingShowcaseMode.BOOKING
    else -> null
}

private fun resolveClassificationVerificationGender(debugScreen: String?): ClassificationGender {
    val suffix = debugScreen
        ?.removePrefix(CLASSIFICATION_UI_VERIFICATION_SCREEN)
        ?.trimStart(':')
        ?.trim()
        ?.lowercase()
    return when (suffix) {
        "male" -> ClassificationGender.MALE
        "female" -> ClassificationGender.FEMALE
        else -> ClassificationGender.ALL
    }
}

private fun rankingShowcaseState(mode: RankingShowcaseMode): RankingUiState {
    val rankingType = when (mode) {
        RankingShowcaseMode.HOT -> RankingType.HOT
        RankingShowcaseMode.RECOMMEND -> RankingType.RECOMMEND
        RankingShowcaseMode.BOOKING -> RankingType.BOOKING
    }
    return RankingUiState(
        selectedContentType = RankingContentType.ALL,
        selectedRankingType = rankingType,
        items = rankingShowcaseItems(mode),
        isLoading = false,
        isRefreshing = false,
        isAppending = false,
        appendErrorMessage = null,
        errorMessage = null,
        page = 1,
        hasNextPage = true,
        hasLoadedOnce = true,
        bookingInFlightIds = emptySet(),
    )
}

private fun rankingShowcaseItems(mode: RankingShowcaseMode): List<RankingDramaItemUiModel> = when (mode) {
    RankingShowcaseMode.HOT -> listOf(
        RankingDramaItemUiModel(
            id = "hot-1",
            rank = 1,
            title = "咱家剑宗团宠小师妹第二季",
            secondaryText = "萌宝",
            description = "“第1季看了好几遍，终于等来了第2季，不容易啊😭😭...”",
            coverUrl = "showcase://hot-1",
            metricLabel = "热度",
            metricValue = "7741万",
            metricVisual = RankingMetricVisual.FLAME,
            detailTags = listOf(
                RankingDetailTagUiModel("387.8万收藏"),
                RankingDetailTagUiModel("1285.3万次点赞"),
            ),
            bookingHintText = null,
            bookingCount = 0,
            isBooked = false,
            posterTitle = "咱家剑宗\n团宠小师\n妹第二季",
            posterStyle = RankingPosterStyle.PEARL,
        ),
        RankingDramaItemUiModel(
            id = "hot-2",
            rank = 2,
            title = "虎妈驾到，全家反骨仔乖乖立正",
            secondaryText = "家庭 · 刘清心 · 冯青青",
            description = "杀猪匠林迎春因名下突然欠了50万网贷被迫讨债，发现是...",
            coverUrl = "showcase://hot-2",
            metricLabel = "热度",
            metricValue = "7676万",
            metricVisual = RankingMetricVisual.FLAME,
            detailTags = listOf(
                RankingDetailTagUiModel("新剧", RankingDetailTagTone.MINT),
                RankingDetailTagUiModel("评分9.3", RankingDetailTagTone.CORAL),
                RankingDetailTagUiModel("151.7万收藏"),
                RankingDetailTagUiModel("342.3万次点赞"),
            ),
            bookingHintText = null,
            bookingCount = 0,
            isBooked = false,
            posterTitle = "虎妈驾到\n全家反骨\n仔乖乖立正",
            posterStyle = RankingPosterStyle.EMERALD,
        ),
        RankingDramaItemUiModel(
            id = "hot-3",
            rank = 3,
            title = "万妖图录传奇第九季",
            secondaryText = "玄幻",
            description = "以妖魔之血为墨，以百妖谱为卷，可摹画其形，夺其神...",
            coverUrl = "showcase://hot-3",
            metricLabel = "热度",
            metricValue = "7253万",
            metricVisual = RankingMetricVisual.FLAME,
            detailTags = listOf(
                RankingDetailTagUiModel("156.6万收藏"),
                RankingDetailTagUiModel("347.3万次点赞"),
            ),
            bookingHintText = null,
            bookingCount = 0,
            isBooked = false,
            posterTitle = "万妖图录\n传奇第九\n季",
            posterStyle = RankingPosterStyle.SCARLET,
        ),
        RankingDramaItemUiModel(
            id = "hot-4",
            rank = 4,
            title = "浙染",
            secondaryText = "爱情 · 郭宇欣 · 张翅",
            description = "因前夫出轨果断离婚的财务总监程凌，在董事长谢叙止...",
            coverUrl = "showcase://hot-4",
            metricLabel = "热度",
            metricValue = "6988万",
            metricVisual = RankingMetricVisual.FLAME,
            detailTags = listOf(
                RankingDetailTagUiModel("新剧", RankingDetailTagTone.MINT),
                RankingDetailTagUiModel("评分9.5", RankingDetailTagTone.CORAL),
                RankingDetailTagUiModel("240.7万收藏"),
                RankingDetailTagUiModel("493.9万次点赞"),
            ),
            bookingHintText = null,
            bookingCount = 0,
            isBooked = false,
            posterTitle = "浙染",
            posterStyle = RankingPosterStyle.SKY,
        ),
        RankingDramaItemUiModel(
            id = "hot-5",
            rank = 5,
            title = "昼以继夜2",
            secondaryText = "爱情 · 梁思伟 · 张晋宜",
            description = "“窝趣！我居然穿成了一名观众，我要做首评！看这个...”",
            coverUrl = "showcase://hot-5",
            metricLabel = "热度",
            metricValue = "6512万",
            metricVisual = RankingMetricVisual.FLAME,
            detailTags = listOf(
                RankingDetailTagUiModel("新剧", RankingDetailTagTone.MINT),
                RankingDetailTagUiModel("评分9.5", RankingDetailTagTone.CORAL),
                RankingDetailTagUiModel("243.1万收藏"),
                RankingDetailTagUiModel("518万次点赞"),
            ),
            bookingHintText = null,
            bookingCount = 0,
            isBooked = false,
            posterTitle = "昼以继夜2",
            posterStyle = RankingPosterStyle.PEARL,
        ),
    )
    RankingShowcaseMode.RECOMMEND -> listOf(
        RankingDramaItemUiModel(
            id = "recommend-1",
            rank = 1,
            title = "全族托举农门状元郎",
            secondaryText = "剧情",
            description = "“好剧，全族都是好的没得勾心斗角，主角懂事努力无...”",
            coverUrl = "showcase://recommend-1",
            metricLabel = "推荐",
            metricValue = "996万",
            metricVisual = RankingMetricVisual.FLAME,
            detailTags = listOf(
                RankingDetailTagUiModel("5009万热度"),
                RankingDetailTagUiModel("87.4万次点赞"),
            ),
            bookingHintText = null,
            bookingCount = 0,
            isBooked = false,
            posterTitle = "全族托举\n农门状元\n郎",
            posterStyle = RankingPosterStyle.SUNSET,
        ),
        RankingDramaItemUiModel(
            id = "recommend-2",
            rank = 2,
            title = "母亲是巨龙我咋是山海异兽呢？",
            secondaryText = "奇幻",
            description = "李烛意外穿越艾尔瑞亚大陆，重生成一只血脉稀薄，被...",
            coverUrl = "showcase://recommend-2",
            metricLabel = "推荐",
            metricValue = "989万",
            metricVisual = RankingMetricVisual.FLAME,
            detailTags = listOf(
                RankingDetailTagUiModel("5216万热度"),
                RankingDetailTagUiModel("100.1万次点赞"),
            ),
            bookingHintText = null,
            bookingCount = 0,
            isBooked = false,
            posterTitle = "母亲是巨\n龙我咋是\n山海异兽",
            posterStyle = RankingPosterStyle.MIDNIGHT,
        ),
        RankingDramaItemUiModel(
            id = "recommend-3",
            rank = 3,
            title = "觉醒十殿阎罗吾即是红伞鬼仙",
            secondaryText = "脑洞",
            description = "异能者江阎觉醒双生神赐，手握SSS级十方鬼令统帅百...",
            coverUrl = "showcase://recommend-3",
            metricLabel = "推荐",
            metricValue = "975万",
            metricVisual = RankingMetricVisual.FLAME,
            detailTags = listOf(
                RankingDetailTagUiModel("5110万热度"),
                RankingDetailTagUiModel("176万次点赞"),
            ),
            bookingHintText = null,
            bookingCount = 0,
            isBooked = false,
            posterTitle = "觉醒十殿\n阎罗吾即\n是红伞鬼",
            posterStyle = RankingPosterStyle.SCARLET,
        ),
        RankingDramaItemUiModel(
            id = "recommend-4",
            rank = 4,
            title = "赠物得长生，老头修仙记！",
            secondaryText = "玄幻",
            description = "“笑死了🤣很好看的.剧情不错.推荐.我先收藏一波”",
            coverUrl = "showcase://recommend-4",
            metricLabel = "推荐",
            metricValue = "967万",
            metricVisual = RankingMetricVisual.FLAME,
            detailTags = listOf(
                RankingDetailTagUiModel("4741万热度"),
                RankingDetailTagUiModel("281.3万次点赞"),
            ),
            bookingHintText = null,
            bookingCount = 0,
            isBooked = false,
            posterTitle = "赠物得长\n生老头修\n仙记",
            posterStyle = RankingPosterStyle.PEARL,
        ),
        RankingDramaItemUiModel(
            id = "recommend-5",
            rank = 5,
            title = "香蜜沉沉烬如霜（上）",
            secondaryText = "爱情 · 王云云 · 张庭睿",
            description = "“穗禾，这次你放胆去做，因为这次你的身后人山人海...”",
            coverUrl = "showcase://recommend-5",
            metricLabel = "推荐",
            metricValue = "960万",
            metricVisual = RankingMetricVisual.FLAME,
            detailTags = listOf(
                RankingDetailTagUiModel("3734万热度"),
                RankingDetailTagUiModel("95.2万次点赞"),
            ),
            bookingHintText = null,
            bookingCount = 0,
            isBooked = false,
            posterTitle = "香蜜沉沉\n烬如霜上",
            posterStyle = RankingPosterStyle.AMBER,
        ),
    )
    RankingShowcaseMode.BOOKING -> listOf(
        RankingDramaItemUiModel(
            id = "booking-1",
            rank = 1,
            title = "昼以继夜3",
            secondaryText = "群像 · 张晋宜 · 梁思伟",
            description = "婚后的日子，有浪漫的心跳，也有柴米油盐的温暖烟火...",
            coverUrl = "showcase://booking-1",
            metricLabel = "期待",
            metricValue = "3364万",
            metricVisual = RankingMetricVisual.CALENDAR,
            detailTags = listOf(
                RankingDetailTagUiModel("198.8万人预约"),
            ),
            bookingHintText = "预告 · 198.8万人预约 · 预计11月上线",
            bookingCount = 1988000,
            isBooked = false,
            posterTitle = "昼以继夜3",
            posterStyle = RankingPosterStyle.AMBER,
        ),
        RankingDramaItemUiModel(
            id = "booking-2",
            rank = 2,
            title = "十八岁太奶奶驾到，重整家族荣光",
            secondaryText = "穿越 · 李柯以 · 屈刚",
            description = "纪家迎来了最震惊的消息：纪家竟有了孙流落在外！为...",
            coverUrl = "showcase://booking-2",
            metricLabel = "期待",
            metricValue = "1910万",
            metricVisual = RankingMetricVisual.CALENDAR,
            detailTags = listOf(
                RankingDetailTagUiModel("1369.4万人预约"),
            ),
            bookingHintText = "预告 · 1369.4万人预约 · 预计12月上线",
            bookingCount = 13694000,
            isBooked = false,
            posterTitle = "十八岁太\n奶奶驾到\n重整家族",
            posterStyle = RankingPosterStyle.SCARLET,
        ),
        RankingDramaItemUiModel(
            id = "booking-3",
            rank = 3,
            title = "少夫人来自东北3",
            secondaryText = "都市爱情 · 梁雯晶 · 业文Kevin",
            description = "东北虎妞小拧嫁入周家，本以为从此只有豪门甜宠，一...",
            coverUrl = "showcase://booking-3",
            metricLabel = "期待",
            metricValue = "1424万",
            metricVisual = RankingMetricVisual.CALENDAR,
            detailTags = listOf(
                RankingDetailTagUiModel("627.4万人预约"),
            ),
            bookingHintText = "预告 · 627.4万人预约 · 预计12月上线",
            bookingCount = 6274000,
            isBooked = false,
            posterTitle = "少夫人来\n自东北3",
            posterStyle = RankingPosterStyle.FOREST,
        ),
        RankingDramaItemUiModel(
            id = "booking-4",
            rank = 4,
            title = "女相师2",
            secondaryText = "东方玄幻 · 孟娜 · 时康",
            description = "上古相师龙问心，曾为救苍生将一身神力散作十二碎片...",
            coverUrl = "showcase://booking-4",
            metricLabel = "期待",
            metricValue = "1132万",
            metricVisual = RankingMetricVisual.CALENDAR,
            detailTags = listOf(
                RankingDetailTagUiModel("1004.7万人预约"),
            ),
            bookingHintText = "预告 · 1004.7万人预约 · 预计7月上线",
            bookingCount = 10047000,
            isBooked = false,
            posterTitle = "女相师2",
            posterStyle = RankingPosterStyle.SCARLET,
        ),
        RankingDramaItemUiModel(
            id = "booking-5",
            rank = 5,
            title = "昼以继夜2游玩篇",
            secondaryText = "职场婚恋 · 梁思伟 · 张晋宜",
            description = "逃离都市喧嚣，沉浸式体验三对绝美CP的高糖治愈之旅...",
            coverUrl = "showcase://booking-5",
            metricLabel = "期待",
            metricValue = "1043万",
            metricVisual = RankingMetricVisual.CALENDAR,
            detailTags = listOf(
                RankingDetailTagUiModel("123.4万人预约"),
            ),
            bookingHintText = "预告 · 123.4万人预约 · 预计7月25日上线",
            bookingCount = 1234000,
            isBooked = false,
            posterTitle = "昼以继夜2\n游玩篇",
            posterStyle = RankingPosterStyle.FOREST,
        ),
    )
}
