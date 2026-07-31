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
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.djs66256.short_drama.core.auth.AuthBootstrapper
import com.djs66256.short_drama.core.theme.ShortDramaTheme
import com.djs66256.short_drama.navigation.DeeplinkRouteParser
import com.djs66256.short_drama.navigation.MainNavigationViewModel
import com.djs66256.short_drama.navigation.NavGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val navigationViewModel: MainNavigationViewModel by viewModels()

    @Inject
    lateinit var authBootstrapper: AuthBootstrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
        enableEdgeToEdge()
        setContent {
            ShortDramaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        navigationViewModel = navigationViewModel,
                    )
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
        handleDeepLink(intent)
    }

    fun handleDeepLink(intent: Intent) {
        val route = DeeplinkRouteParser.parse(intent.data) ?: return
        navigationViewModel.enqueuePendingRoute(route)
    }
}
