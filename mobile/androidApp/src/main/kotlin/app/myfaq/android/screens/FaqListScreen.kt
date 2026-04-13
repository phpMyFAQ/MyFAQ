package app.myfaq.android.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.myfaq.android.screens.components.ErrorRetry
import app.myfaq.android.screens.components.FaqCard
import app.myfaq.android.screens.components.LoadingIndicator
import app.myfaq.shared.data.ActiveInstanceManager
import app.myfaq.shared.ui.CategoriesViewModel
import app.myfaq.shared.ui.UiState
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqListScreen(
    categoryId: Int,
    categoryName: String,
    onFaqClick: (faqId: Int) -> Unit,
    onBack: () -> Unit,
    aim: ActiveInstanceManager = koinInject(),
) {
    val vm = remember { CategoriesViewModel(aim) }
    val state by vm.faqList.collectAsState()

    LaunchedEffect(categoryId) { vm.loadFaqsForCategory(categoryId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(categoryName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when (val s = state) {
            is UiState.Loading -> LoadingIndicator(modifier = Modifier.padding(padding))
            is UiState.Error ->
                ErrorRetry(
                    message = s.message,
                    onRetry = { vm.loadFaqsForCategory(categoryId) },
                    modifier = Modifier.padding(padding),
                )
            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(s.data, key = { it.id }) { faq ->
                        FaqCard(
                            question = faq.question,
                            updated = faq.updated,
                            onClick = { onFaqClick(faq.id) },
                        )
                    }
                }
            }
        }
    }
}
