package com.example.watchgomoku.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.watchgomoku.GomokuApplication
import com.example.watchgomoku.data.local.resolvedGraphics
import com.example.watchgomoku.domain.model.AiDifficulty
import com.example.watchgomoku.domain.model.FirstMovePolicy
import com.example.watchgomoku.domain.model.GameMode
import com.example.watchgomoku.domain.model.TransitionStyle
import com.example.watchgomoku.presentation.about.AboutScreen
import com.example.watchgomoku.presentation.game.GameRoute
import com.example.watchgomoku.presentation.history.HistoryScreen
import com.example.watchgomoku.presentation.history.HistoryViewModel
import com.example.watchgomoku.presentation.home.HomeScreen
import com.example.watchgomoku.presentation.home.HomeViewModel
import com.example.watchgomoku.presentation.mode.DifficultyScreen
import com.example.watchgomoku.presentation.mode.ModeSelectScreen
import com.example.watchgomoku.presentation.settings.SettingsCategory
import com.example.watchgomoku.presentation.settings.SettingsDetailScreen
import com.example.watchgomoku.presentation.settings.SettingsScreen
import com.example.watchgomoku.presentation.settings.SettingsViewModel
import com.example.watchgomoku.presentation.splash.SplashScreen
import com.example.watchgomoku.ui.theme.rememberReducedMotion

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val MODE = "mode"
    const val SETTINGS = "settings"
    const val SETTINGS_DETAIL = "settings/{category}"
    fun settingsDetail(category: SettingsCategory) = "settings/${category.name}"
    const val HISTORY = "history"
    const val ABOUT = "about"
    const val DIFFICULTY = "difficulty/{mode}"
    const val GAME = "game/{mode}/{difficulty}/{first}/{size}/{restore}"

    fun difficulty(mode: GameMode): String = "difficulty/${mode.name}"

    fun game(
        mode: GameMode,
        difficulty: AiDifficulty?,
        first: FirstMovePolicy?,
        size: Int,
        restore: Boolean = false
    ): String = "game/${mode.name}/" +
        "${difficulty?.name ?: AiDifficulty.NORMAL.name}/" +
        "${first?.name ?: FirstMovePolicy.PLAYER_BLACK.name}/" +
        "${size.coerceIn(9, 19)}/$restore"
}

@Composable
fun GomokuNavHost(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    // 全局页面切换动画：曲线变速，由设置“页面转场”与系统减少动效共同决定；
    // 不影响启动加载动画（独立页面，不走此转场）
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val reducedMotion = rememberReducedMotion()
    val transitionStyle = settings.transitionStyle
    val enterDur = when {
        reducedMotion || transitionStyle == TransitionStyle.OFF -> 1
        transitionStyle == TransitionStyle.SOFT -> 120
        else -> 220
    }
    val exitDur = when {
        reducedMotion || transitionStyle == TransitionStyle.OFF -> 1
        transitionStyle == TransitionStyle.SOFT -> 100
        else -> 160
    }
    val withSlide = !reducedMotion && transitionStyle == TransitionStyle.FULL

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier,
        enterTransition = {
            fadeIn(tween(enterDur, easing = EaseOutCubic)) +
                if (withSlide) {
                    slideInVertically(tween(enterDur, easing = EaseOutCubic)) { it / 20 }
                } else {
                    EnterTransition.None
                }
        },
        exitTransition = { fadeOut(tween(exitDur)) },
        popEnterTransition = { fadeIn(tween(enterDur, easing = EaseOutCubic)) },
        popExitTransition = {
            fadeOut(tween(exitDur)) +
                if (withSlide) {
                    slideOutVertically(tween(exitDur)) { it / 20 }
                } else {
                    ExitTransition.None
                }
        }
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            val app = LocalContext.current.applicationContext as GomokuApplication
            val homeViewModel: HomeViewModel = viewModel { HomeViewModel(app.container) }
            HomeScreen(
                viewModel = homeViewModel,
                graphics = settings.resolvedGraphics(),
                onSolo = { navController.navigate(Routes.MODE) },
                onMulti = {
                    navController.navigate(
                        Routes.game(
                            GameMode.MULTIPLAYER, null, null,
                            homeViewModel.boardSize.value
                        )
                    )
                },
                onPractice = {
                    navController.navigate(
                        Routes.game(
                            GameMode.PRACTICE, null, null,
                            homeViewModel.boardSize.value
                        )
                    )
                },
                onContinue = {
                    val saved = homeViewModel.savedGame.value
                    if (saved != null) {
                        navController.navigate(
                            Routes.game(
                                mode = runCatching { GameMode.valueOf(saved.mode) }
                                    .getOrDefault(GameMode.SINGLE_PLAYER),
                                difficulty = saved.difficulty?.let {
                                    runCatching { AiDifficulty.valueOf(it) }.getOrNull()
                                },
                                first = saved.firstPolicy?.let {
                                    runCatching { FirstMovePolicy.valueOf(it) }.getOrNull()
                                },
                                size = saved.boardSize,
                                restore = true
                            )
                        )
                    }
                },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onHistory = { navController.navigate(Routes.HISTORY) },
                onAbout = { navController.navigate(Routes.ABOUT) }
            )
        }

        composable(Routes.MODE) {
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            ModeSelectScreen(
                onSolo = { navController.navigate(Routes.difficulty(GameMode.SINGLE_PLAYER)) },
                onMulti = { navController.navigate(Routes.game(GameMode.MULTIPLAYER, null, null, settings.boardSize)) },
                onPractice = { navController.navigate(Routes.game(GameMode.PRACTICE, null, null, settings.boardSize)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DIFFICULTY) { entry ->
            val mode = runCatching {
                GameMode.valueOf(entry.arguments?.getString("mode") ?: "")
            }.getOrDefault(GameMode.SINGLE_PLAYER)
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            DifficultyScreen(
                mode = mode,
                defaultDifficulty = settings.difficulty,
                defaultFirstPolicy = settings.firstMovePolicy,
                defaultBoardSize = settings.boardSize,
                onStart = { difficulty, first, size ->
                    navController.navigate(
                        Routes.game(GameMode.SINGLE_PLAYER, difficulty, first, size)
                    )
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onCategory = { category ->
                    navController.navigate(Routes.settingsDetail(category))
                },
                onDone = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS_DETAIL) { entry ->
            val category = entry.arguments?.getString("category")?.let {
                runCatching { SettingsCategory.valueOf(it) }.getOrNull()
            } ?: SettingsCategory.APPEARANCE
            SettingsDetailScreen(
                category = category,
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.HISTORY) {
            val app = LocalContext.current.applicationContext as GomokuApplication
            val historyViewModel: HistoryViewModel =
                viewModel { HistoryViewModel(app.container) }
            HistoryScreen(
                viewModel = historyViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.GAME) { entry ->
            val mode = runCatching {
                GameMode.valueOf(entry.arguments?.getString("mode") ?: "")
            }.getOrDefault(GameMode.SINGLE_PLAYER)
            val difficulty = entry.arguments?.getString("difficulty")?.let {
                runCatching { AiDifficulty.valueOf(it) }.getOrNull()
            } ?: AiDifficulty.NORMAL
            val first = entry.arguments?.getString("first")?.let {
                runCatching { FirstMovePolicy.valueOf(it) }.getOrNull()
            } ?: FirstMovePolicy.PLAYER_BLACK
            val size = entry.arguments?.getString("size")?.toIntOrNull()?.coerceIn(9, 19) ?: 15
            val restore = entry.arguments?.getString("restore") == "true"

            GameRoute(
                mode = mode,
                difficulty = difficulty,
                firstPolicy = first,
                boardSize = size,
                restore = restore,
                settingsViewModel = settingsViewModel,
                onExit = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
                onViewHistory = { navController.navigate(Routes.HISTORY) }
            )
        }
    }
}
