package app.myfaq.android.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.myfaq.android.screens.components.ErrorRetry
import app.myfaq.android.screens.components.FaqCard
import app.myfaq.android.screens.components.LoadingIndicator
import app.myfaq.shared.api.dto.FaqPopularItem
import app.myfaq.shared.api.dto.NewsItem
import app.myfaq.shared.data.ActiveInstanceManager
import app.myfaq.shared.ui.HomeViewModel
import app.myfaq.shared.ui.UiState
import org.koin.compose.koinInject

private enum class HomeTab(val label: String) {
    Sticky("Sticky"),
    Popular("Popular"),
    Latest("Latest"),
    News("News"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onFaqClick: (categoryId: Int, faqId: Int) -> Unit,
    aim: ActiveInstanceManager = koinInject(),
) {
    val vm = remember { HomeViewModel(aim) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.loadAll() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("MyFAQ") })
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
            ) {
                HomeTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(tab.label) },
                    )
                }
            }

            when (HomeTab.entries[selectedTab]) {
                HomeTab.Sticky -> FaqTabContent(
                    state = vm.sticky.collectAsState().value,
                    onRetry = { vm.loadSticky() },
                    onRefresh = { vm.loadSticky() },
                    onItemClick = { item ->
                        item.url?.let { url ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    },
                )
                HomeTab.Popular -> FaqTabContent(
                    state = vm.popular.collectAsState().value,
                    onRetry = { vm.loadPopular() },
                    onRefresh = { vm.loadPopular() },
                    onItemClick = { item ->
                        item.url?.let { url ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    },
                )
                HomeTab.Latest -> FaqTabContent(
                    state = vm.latest.collectAsState().value,
                    onRetry = { vm.loadLatest() },
                    onRefresh = { vm.loadLatest() },
                    onItemClick = { item ->
                        item.url?.let { url ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    },
                )
                HomeTab.News -> NewsTabContent(
                    state = vm.news.collectAsState().value,
                    onRetry = { vm.loadNews() },
                    onRefresh = { vm.loadNews() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FaqTabContent(
    state: UiState<List<FaqPopularItem>>,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onItemClick: (FaqPopularItem) -> Unit,
) {
    when (state) {
        is UiState.Loading -> LoadingIndicator()
        is UiState.Error -> ErrorRetry(message = state.message, onRetry = onRetry)
        is UiState.Success -> {
            var isRefreshing by remember { mutableStateOf(false) }
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    onRefresh()
                    isRefreshing = false
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.data.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) {
                        Text("No FAQs found", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.data, key = { it.id ?: it.question.hashCode() }) { faq ->
                            FaqCard(
                                question = faq.question,
                                updated = faq.date,
                                onClick = { onItemClick(faq) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewsTabContent(
    state: UiState<List<NewsItem>>,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
) {
    when (state) {
        is UiState.Loading -> LoadingIndicator()
        is UiState.Error -> ErrorRetry(message = state.message, onRetry = onRetry)
        is UiState.Success -> {
            var isRefreshing by remember { mutableStateOf(false) }
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    onRefresh()
                    isRefreshing = false
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.data.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) {
                        Text("No news", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.data, key = { it.id }) { news ->
                            NewsCard(news)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsCard(news: NewsItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = news.header,
                style = MaterialTheme.typography.titleSmall,
            )
            if (!news.date.isNullOrBlank()) {
                Text(
                    text = news.date!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
