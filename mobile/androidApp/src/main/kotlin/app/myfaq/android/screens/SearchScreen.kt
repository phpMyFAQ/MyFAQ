package app.myfaq.android.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.myfaq.android.screens.components.ErrorRetry
import app.myfaq.android.screens.components.LoadingIndicator
import app.myfaq.shared.data.ActiveInstanceManager
import app.myfaq.shared.domain.HtmlUtils
import app.myfaq.shared.ui.SearchViewModel
import app.myfaq.shared.ui.UiState
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onFaqClick: (categoryId: Int, faqId: Int) -> Unit,
    aim: ActiveInstanceManager = koinInject(),
) {
    val vm = remember { SearchViewModel(aim) }
    val query by vm.query.collectAsState()
    val resultsState by vm.results.collectAsState()
    val popularState by vm.popularSearches.collectAsState()

    LaunchedEffect(Unit) { vm.loadPopularSearches() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Search") })
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.onQueryChanged(it) },
                placeholder = { Text("Search FAQs...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.onQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (query.isBlank()) {
                // Show popular searches
                when (val s = popularState) {
                    is UiState.Loading -> LoadingIndicator()
                    is UiState.Error -> {} // Silently ignore popular search errors
                    is UiState.Success -> {
                        if (s.data.isNotEmpty()) {
                            Text(
                                text = "Popular searches",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                            LazyColumn {
                                items(s.data, key = { it.searchTerm }) { popular ->
                                    ListItem(
                                        headlineContent = { Text(popular.searchTerm) },
                                        modifier =
                                            Modifier.clickable {
                                                vm.onQueryChanged(popular.searchTerm)
                                            },
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            } else {
                // Show search results
                when (val s = resultsState) {
                    is UiState.Loading -> LoadingIndicator()
                    is UiState.Error ->
                        ErrorRetry(
                            message = s.message,
                            onRetry = { vm.onQueryChanged(query) },
                        )
                    is UiState.Success -> {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            items(s.data, key = { it.id }) { result ->
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = HtmlUtils.decodeEntities(result.question),
                                            maxLines = 2,
                                        )
                                    },
                                    supportingContent = {
                                        val preview = result.answer?.let { HtmlUtils.preview(it) }
                                        if (!preview.isNullOrBlank()) {
                                            Text(
                                                text = preview,
                                                maxLines = 2,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    modifier =
                                        Modifier.clickable {
                                            onFaqClick(result.categoryId, result.id)
                                        },
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
