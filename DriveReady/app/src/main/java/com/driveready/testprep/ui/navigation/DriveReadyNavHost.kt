package com.driveready.testprep.ui.navigation

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.dp
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.driveready.testprep.AppViewModel
import com.driveready.testprep.BuildConfig
import com.driveready.testprep.ui.components.openUrl
import com.driveready.testprep.ui.progress.AboutScreen
import com.driveready.testprep.ads.AdsManager
import com.driveready.testprep.ads.BannerAd
import com.driveready.testprep.ads.InterstitialAdManager
import com.driveready.testprep.data.content.HazardClips
import com.driveready.testprep.data.content.Maneuvers
import com.driveready.testprep.data.content.countryById
import com.driveready.testprep.data.local.UserSettings
import com.driveready.testprep.data.model.Country
import com.driveready.testprep.ui.components.AppIcons
import com.driveready.testprep.ui.components.ExitDialog
import com.driveready.testprep.ui.components.openPlayStore
import com.driveready.testprep.ui.country.CountryPickerScreen
import com.driveready.testprep.ui.country.RegionPickerScreen
import com.driveready.testprep.ui.hazard.HazardHubScreen
import com.driveready.testprep.ui.hazard.HazardPlayerScreen
import com.driveready.testprep.ui.home.HomeAction
import com.driveready.testprep.ui.home.HomeScreen
import com.driveready.testprep.ui.onboarding.GetStartedScreen
import com.driveready.testprep.ui.onboarding.GuideScreen
import com.driveready.testprep.ui.onboarding.SplashScreen
import com.driveready.testprep.ui.oral.OralScreen
import com.driveready.testprep.ui.practical.ManeuverScreen
import com.driveready.testprep.ui.practical.PracticalScreen
import com.driveready.testprep.ui.practical.TestDayScreen
import com.driveready.testprep.ui.progress.ProgressScreen
import com.driveready.testprep.ui.progress.SettingsScreen
import com.driveready.testprep.ui.signs.SignsScreen
import com.driveready.testprep.ui.syllabus.SyllabusScreen
import com.driveready.testprep.ui.theme.Brand
import com.driveready.testprep.ui.theory.QuizScreen
import com.driveready.testprep.ui.theory.ResultScreen
import com.driveready.testprep.ui.theory.TheoryHubScreen

object Routes {
    const val SPLASH = "splash"
    const val GUIDE = "guide"
    const val GET_STARTED = "get_started"
    const val COUNTRY = "country?change={change}"
    const val REGION = "region/{countryId}?change={change}"
    const val HOME = "home"
    const val THEORY = "theory"
    const val HAZARD = "hazard"
    const val ORAL = "oral"
    const val PRACTICAL = "practical"
    const val QUIZ = "quiz"
    const val RESULT = "result"
    const val SIGNS = "signs"
    const val SYLLABUS = "syllabus"
    const val MANEUVER = "maneuver/{id}"
    const val HAZARD_PLAY = "hazard_play/{start}"
    const val TEST_DAY = "test_day"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"
    const val ABOUT = "about"

    fun country(change: Boolean) = "country?change=$change"
    fun region(countryId: String, change: Boolean) = "region/$countryId?change=$change"
    fun maneuver(id: String) = "maneuver/$id"
    fun hazardPlay(start: Int) = "hazard_play/$start"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", AppIcons.Home),
    Tab(Routes.THEORY, "Theory", AppIcons.Book),
    Tab(Routes.HAZARD, "Hazard", AppIcons.Eye),
    Tab(Routes.ORAL, "Oral", AppIcons.Mic),
    Tab(Routes.PRACTICAL, "Practical", AppIcons.Wheel)
)

// No banner where it would sit right under answer/navigation buttons, or on intro screens.
private val noBannerRoutes = setOf(Routes.SPLASH, Routes.GUIDE, Routes.GET_STARTED, Routes.HAZARD_PLAY, Routes.QUIZ)

@Composable
fun DriveReadyNavHost(viewModel: AppViewModel) {
    val nav = rememberNavController()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val attempts by viewModel.attempts.collectAsStateWithLifecycle()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val activity = LocalActivity.current
    val context = LocalContext.current
    var showExit by remember { mutableStateOf(false) }

    val isTab = tabs.any { it.route == route }
    val showBanner = route != null && route !in noBannerRoutes

    /** Runs [action] behind an interstitial (respecting the 30 s cap) on major actions. */
    fun withInterstitial(action: () -> Unit) {
        if (activity != null) InterstitialAdManager.showThen(activity, action) else action()
    }

    Scaffold(
        containerColor = Brand.Surface,
        bottomBar = {
            Column(if (!isTab) Modifier.navigationBarsPadding() else Modifier) {
                if (showBanner) {
                    // Visual separation keeps the banner clear of on-screen buttons.
                    HorizontalDivider(color = Brand.Line)
                    BannerAd(Modifier.padding(top = 6.dp, bottom = if (isTab) 2.dp else 0.dp))
                }
                if (isTab) {
                    NavigationBar(containerColor = Brand.Card) {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = route == tab.route,
                                onClick = { nav.navigateTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Brand.Ink,
                                    indicatorColor = Brand.Amber,
                                    selectedTextColor = Brand.Ink,
                                    unselectedIconColor = Brand.Muted,
                                    unselectedTextColor = Brand.Muted
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = Routes.SPLASH) {
            composable(Routes.SPLASH) {
                SplashScreen(onFinished = {
                    val s = viewModel.settings.value
                    val next = when {
                        s == null || !s.onboardingDone -> Routes.GUIDE
                        countryById(s.countryId) == null -> Routes.country(false)
                        else -> Routes.HOME
                    }
                    nav.navigate(next) { popUpTo(Routes.SPLASH) { inclusive = true } }
                })
            }
            composable(Routes.GUIDE) {
                GuideScreen(onDone = { nav.navigate(Routes.GET_STARTED) { popUpTo(Routes.GUIDE) { inclusive = true } } })
            }
            composable(Routes.GET_STARTED) {
                GetStartedScreen(onGetStarted = { nav.navigate(Routes.country(false)) })
            }
            composable(Routes.COUNTRY, arguments = listOf(navArgument("change") { type = NavType.BoolType; defaultValue = false })) { entry ->
                val change = entry.arguments?.getBoolean("change") == true
                CountryPickerScreen(
                    selectedId = settings?.countryId,
                    onBack = if (change || nav.previousBackStackEntry != null) ({ nav.popBackStack() }) else null,
                    onPick = { country ->
                        if (country.hasRegions) nav.navigate(Routes.region(country.id, change))
                        else {
                            val commit = {
                                viewModel.selectCountry(country.id, null)
                                viewModel.completeOnboarding()
                                nav.goHome()
                            }
                            if (change) withInterstitial(commit) else commit()
                        }
                    },
                    contentPadding = padding
                )
            }
            composable(
                Routes.REGION,
                arguments = listOf(
                    navArgument("countryId") { type = NavType.StringType },
                    navArgument("change") { type = NavType.BoolType; defaultValue = false }
                )
            ) { entry ->
                val country = countryById(entry.arguments?.getString("countryId")) ?: return@composable
                val change = entry.arguments?.getBoolean("change") == true
                RegionPickerScreen(
                    country = country,
                    selectedId = settings?.regionId,
                    onBack = { nav.popBackStack() },
                    onPick = { region ->
                        val commit = {
                            viewModel.selectCountry(country.id, region.id)
                            viewModel.completeOnboarding()
                            nav.goHome()
                        }
                        if (change) withInterstitial(commit) else commit()
                    },
                    contentPadding = padding
                )
            }

            composable(Routes.HOME) {
                WithCountry(viewModel, settings) { country, s ->
                    BackHandler { showExit = true }
                    HomeScreen(
                        country = country,
                        region = viewModel.region(s),
                        settings = s,
                        readiness = viewModel.readiness(country, s, attempts),
                        contentPadding = padding,
                        onAction = { action ->
                            when (action) {
                                HomeAction.MOCK -> startMock(viewModel, country, s, nav, ::withInterstitial)
                                HomeAction.PRACTICE -> nav.navigateTab(Routes.THEORY)
                                HomeAction.SYLLABUS -> nav.navigate(Routes.SYLLABUS)
                                HomeAction.SIGNS -> nav.navigate(Routes.SIGNS)
                                HomeAction.HAZARD -> nav.navigateTab(Routes.HAZARD)
                                HomeAction.ORAL -> nav.navigateTab(Routes.ORAL)
                                HomeAction.PRACTICAL -> nav.navigateTab(Routes.PRACTICAL)
                                HomeAction.TEST_DAY -> nav.navigate(Routes.TEST_DAY)
                                HomeAction.PROGRESS -> nav.navigate(Routes.PROGRESS)
                                HomeAction.SETTINGS -> nav.navigate(Routes.SETTINGS)
                                HomeAction.CHANGE_COUNTRY -> nav.navigate(Routes.country(true))
                            }
                        },
                        onSetTestDate = { viewModel.setTestDate(it) }
                    )
                }
            }
            composable(Routes.THEORY) {
                WithCountry(viewModel, settings) { country, s ->
                    TheoryHubScreen(
                        country = country,
                        region = viewModel.region(s),
                        poolSize = viewModel.questionPool(country).size,
                        contentPadding = padding,
                        onMock = { startMock(viewModel, country, s, nav, ::withInterstitial) },
                        onPractice = { cat ->
                            viewModel.activeQuiz = viewModel.practiceConfig(country, cat)
                            nav.navigate(Routes.QUIZ)
                        },
                        onSigns = { nav.navigate(Routes.SIGNS) },
                        onSyllabus = { nav.navigate(Routes.SYLLABUS) }
                    )
                }
            }
            composable(Routes.QUIZ) {
                WithCountry(viewModel, settings) { country, _ ->
                    val config = viewModel.activeQuiz
                    if (config == null) {
                        LaunchedEffect(Unit) { nav.popBackStack() }
                        return@WithCountry
                    }
                    QuizScreen(
                        config = config,
                        country = country,
                        contentPadding = padding,
                        onExit = { nav.popBackStack() },
                        onFinish = { result, userInitiated ->
                            viewModel.finishQuiz(country, result)
                            val toResult = { nav.navigate(Routes.RESULT) { popUpTo(Routes.QUIZ) { inclusive = true } } }
                            // Interstitials only follow a user action, never the timer running out.
                            if (userInitiated) withInterstitial(toResult) else toResult()
                        }
                    )
                }
            }
            composable(Routes.RESULT) {
                WithCountry(viewModel, settings) { country, s ->
                    val result = viewModel.lastResult
                    if (result == null) {
                        LaunchedEffect(Unit) { nav.popBackStack() }
                        return@WithCountry
                    }
                    ResultScreen(
                        result = result,
                        country = country,
                        contentPadding = padding,
                        onDone = { nav.popBackStack() },
                        onRetry = {
                            viewModel.activeQuiz = if (result.config.isMock) viewModel.mockConfig(country, viewModel.region(s))
                            else result.config.copy(questions = result.config.questions.shuffled())
                            nav.navigate(Routes.QUIZ) { popUpTo(Routes.RESULT) { inclusive = true } }
                        }
                    )
                }
            }
            composable(Routes.SIGNS) {
                WithCountry(viewModel, settings) { country, s ->
                    SignsScreen(
                        country = country,
                        mastered = s.masteredSigns,
                        contentPadding = padding,
                        onBack = { nav.popBackStack() },
                        onToggleMastered = { viewModel.toggleSign(country, it) },
                        onQuiz = {
                            viewModel.activeQuiz = viewModel.practiceConfig(country, com.driveready.testprep.data.model.QuestionCategory.SIGNS)
                            nav.navigate(Routes.QUIZ)
                        }
                    )
                }
            }
            composable(Routes.SYLLABUS) {
                WithCountry(viewModel, settings) { country, s ->
                    SyllabusScreen(
                        country = country,
                        region = viewModel.region(s),
                        done = s.syllabusDone,
                        contentPadding = padding,
                        onBack = { nav.popBackStack() },
                        onToggle = { viewModel.toggleSyllabus(country, it) }
                    )
                }
            }
            composable(Routes.HAZARD) {
                WithCountry(viewModel, settings) { country, _ ->
                    HazardHubScreen(
                        country = country,
                        bestScore = attempts.filter { it.type == "hazard" }.maxOfOrNull { it.score },
                        contentPadding = padding,
                        onStartTest = { withInterstitial { nav.navigate(Routes.hazardPlay(-1)) } },
                        onPlayClip = { nav.navigate(Routes.hazardPlay(it)) }
                    )
                }
            }
            composable(Routes.HAZARD_PLAY, arguments = listOf(navArgument("start") { type = NavType.IntType })) { entry ->
                WithCountry(viewModel, settings) { country, _ ->
                    val start = entry.arguments?.getInt("start") ?: -1
                    val clips = if (start in HazardClips.indices) listOf(HazardClips[start]) else HazardClips
                    HazardPlayerScreen(
                        country = country,
                        clips = clips,
                        contentPadding = padding,
                        onExit = { nav.popBackStack() },
                        onFinished = { score, total, passed ->
                            if (clips.size == HazardClips.size) viewModel.saveHazard(country, score, total, passed)
                        }
                    )
                }
            }
            composable(Routes.ORAL) {
                WithCountry(viewModel, settings) { country, s ->
                    OralScreen(
                        country = country,
                        confident = s.oralConfident,
                        contentPadding = padding,
                        onToggleConfident = { viewModel.toggleOral(country, it) }
                    )
                }
            }
            composable(Routes.PRACTICAL) {
                WithCountry(viewModel, settings) { country, s ->
                    PracticalScreen(
                        country = country,
                        confident = s.maneuversConfident,
                        contentPadding = padding,
                        onOpenManeuver = { id -> nav.navigate(Routes.maneuver(id)) },
                        onTestDay = { nav.navigate(Routes.TEST_DAY) }
                    )
                }
            }
            composable(Routes.MANEUVER, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                WithCountry(viewModel, settings) { country, s ->
                    val m = Maneuvers[entry.arguments?.getString("id")] ?: return@WithCountry
                    ManeuverScreen(
                        country = country,
                        maneuver = m,
                        confident = "${country.id}:${m.id}" in s.maneuversConfident,
                        contentPadding = padding,
                        onBack = { nav.popBackStack() },
                        onToggleConfident = { viewModel.toggleManeuver(country, m.id) }
                    )
                }
            }
            composable(Routes.TEST_DAY) {
                WithCountry(viewModel, settings) { country, s ->
                    TestDayScreen(
                        country = country,
                        done = s.checklistDone,
                        contentPadding = padding,
                        onBack = { nav.popBackStack() },
                        onToggle = { viewModel.toggleChecklist(country, it) },
                        onWarmUp = { nav.navigateTab(Routes.ORAL) }
                    )
                }
            }
            composable(Routes.PROGRESS) {
                WithCountry(viewModel, settings) { country, s ->
                    ProgressScreen(
                        country = country,
                        settings = s,
                        attempts = attempts,
                        readiness = viewModel.readiness(country, s, attempts),
                        contentPadding = padding,
                        onBack = { nav.popBackStack() }
                    )
                }
            }
            composable(Routes.SETTINGS) {
                WithCountry(viewModel, settings) { country, s ->
                    SettingsScreen(
                        country = country,
                        region = viewModel.region(s),
                        showPrivacyOptions = AdsManager.isPrivacyOptionsRequired,
                        contentPadding = padding,
                        onBack = { nav.popBackStack() },
                        onChangeCountry = { nav.navigate(Routes.country(true)) },
                        onPrivacy = { activity?.let { AdsManager.showPrivacyOptions(it) } },
                        onRate = { openPlayStore(context) },
                        onPrivacyPolicy = { openUrl(context, BuildConfig.PRIVACY_POLICY_URL) },
                        onAbout = { nav.navigate(Routes.ABOUT) },
                        onShare = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "I'm preparing for my driving test with DriveReady: https://play.google.com/store/apps/details?id=${context.packageName}")
                            }
                            context.startActivity(Intent.createChooser(send, "Share DriveReady").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    )
                }
            }
            composable(Routes.ABOUT) {
                AboutScreen(
                    contentPadding = padding,
                    onBack = { nav.popBackStack() },
                    onPrivacyPolicy = { openUrl(context, BuildConfig.PRIVACY_POLICY_URL) }
                )
            }
        }
    }

    if (showExit) {
        ExitDialog(
            onExit = { showExit = false; activity?.finish() },
            onCancel = { showExit = false },
            onRate = { showExit = false; openPlayStore(context) }
        )
    }
}

/** Renders [content] once the user's country is known; otherwise sends them to the picker. */
@Composable
private fun WithCountry(
    viewModel: AppViewModel,
    settings: UserSettings?,
    content: @Composable (Country, UserSettings) -> Unit
) {
    val s = settings ?: return
    val country = viewModel.country(s) ?: return
    content(country, s)
}

private fun startMock(
    viewModel: AppViewModel,
    country: Country,
    settings: UserSettings,
    nav: NavHostController,
    withInterstitial: (() -> Unit) -> Unit
) {
    viewModel.activeQuiz = viewModel.mockConfig(country, viewModel.region(settings))
    withInterstitial { nav.navigate(Routes.QUIZ) }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.goHome() {
    navigate(Routes.HOME) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
