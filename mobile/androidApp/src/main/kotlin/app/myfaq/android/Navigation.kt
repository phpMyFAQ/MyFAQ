package app.myfaq.android

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.myfaq.android.screens.AddInstanceSheet
import app.myfaq.android.screens.CategoriesScreen
import app.myfaq.android.screens.FaqDetailScreen
import app.myfaq.android.screens.FaqListScreen
import app.myfaq.android.screens.HomeScreen
import app.myfaq.android.screens.NewsDetailScreen
import app.myfaq.android.screens.PaywallScreen
import app.myfaq.android.screens.SearchScreen
import app.myfaq.android.screens.SettingsScreen
import app.myfaq.android.screens.WorkspacesScreen
import app.myfaq.shared.data.ActiveInstanceManager
import org.koin.compose.koinInject
import java.net.URLDecoder
import java.net.URLEncoder

// ── Route constants ────────────────────────────────────────────────

object Routes {
    const val WORKSPACES = "workspaces"
    const val ADD_INSTANCE = "add_instance"
    const val HOME = "home"
    const val CATEGORIES = "categories"
    const val FAQ_LIST = "categories/{categoryId}/{categoryName}"
    const val FAQ_DETAIL = "faq/{categoryId}/{faqId}"
    const val NEWS_DETAIL = "news/{newsId}"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val PAYWALL = "paywall"

    fun faqList(
        categoryId: Int,
        categoryName: String,
    ): String = "categories/$categoryId/${URLEncoder.encode(categoryName, "UTF-8")}"

    fun faqDetail(
        categoryId: Int,
        faqId: Int,
    ): String = "faq/$categoryId/$faqId"

    fun newsDetail(newsId: Int): String = "news/$newsId"
}

// ── Bottom-bar tabs ────────────────────────────────────────────────

enum class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home(Routes.HOME, "Home", Icons.Default.Home),
    Categories(Routes.CATEGORIES, "Categories", Icons.Default.List),
    Search(Routes.SEARCH, "Search", Icons.Default.Search),
    Settings(Routes.SETTINGS, "Settings", Icons.Default.Settings),
}

// ── Root scaffold with NavHost ─────────────────────────────────────

@Composable
fun MyFaqNavHost(aim: ActiveInstanceManager = koinInject()) {
    val navController = rememberNavController()
    val activeInstance by aim.activeInstance.collectAsState()
    val hasInstance = activeInstance != null

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = hasInstance && currentRoute in BottomTab.entries.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomTab.entries.forEach { tab ->
                        NavigationBarItem(
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (hasInstance) Routes.HOME else Routes.WORKSPACES,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.WORKSPACES) {
                WorkspacesScreen(
                    onInstanceSelected = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.WORKSPACES) { inclusive = true }
                        }
                    },
                    onAddInstance = {
                        navController.navigate(Routes.ADD_INSTANCE)
                    },
                )
            }

            composable(Routes.ADD_INSTANCE) {
                AddInstanceSheet(
                    onDismiss = { navController.popBackStack() },
                    onInstanceAdded = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.WORKSPACES) { inclusive = true }
                        }
                    },
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    onFaqClick = { categoryId, faqId ->
                        navController.navigate(Routes.faqDetail(categoryId, faqId))
                    },
                    onNewsClick = { newsId ->
                        navController.navigate(Routes.newsDetail(newsId))
                    },
                )
            }

            composable(Routes.CATEGORIES) {
                CategoriesScreen(
                    onCategoryClick = { categoryId, categoryName ->
                        navController.navigate(Routes.faqList(categoryId, categoryName))
                    },
                )
            }

            composable(
                route = Routes.FAQ_LIST,
                arguments =
                    listOf(
                        navArgument("categoryId") { type = NavType.IntType },
                        navArgument("categoryName") { type = NavType.StringType },
                    ),
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getInt("categoryId") ?: 0
                val categoryName =
                    backStackEntry.arguments
                        ?.getString("categoryName")
                        ?.let { URLDecoder.decode(it, "UTF-8") } ?: ""
                FaqListScreen(
                    categoryId = categoryId,
                    categoryName = categoryName,
                    onFaqClick = { faqId ->
                        navController.navigate(Routes.faqDetail(categoryId, faqId))
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Routes.FAQ_DETAIL,
                arguments =
                    listOf(
                        navArgument("categoryId") { type = NavType.IntType },
                        navArgument("faqId") { type = NavType.IntType },
                    ),
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getInt("categoryId") ?: 0
                val faqId = backStackEntry.arguments?.getInt("faqId") ?: 0
                FaqDetailScreen(
                    categoryId = categoryId,
                    faqId = faqId,
                    onBack = { navController.popBackStack() },
                    onPaywall = { navController.navigate(Routes.PAYWALL) },
                )
            }

            composable(
                route = Routes.NEWS_DETAIL,
                arguments = listOf(navArgument("newsId") { type = NavType.IntType }),
            ) { backStackEntry ->
                val newsId = backStackEntry.arguments?.getInt("newsId") ?: 0
                NewsDetailScreen(
                    newsId = newsId,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.SEARCH) {
                SearchScreen(
                    onFaqClick = { categoryId, faqId ->
                        navController.navigate(Routes.faqDetail(categoryId, faqId))
                    },
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onSwitchInstance = {
                        navController.navigate(Routes.WORKSPACES) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                        }
                    },
                )
            }

            composable(Routes.PAYWALL) {
                PaywallScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
