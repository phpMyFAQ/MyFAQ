package app.myfaq.android.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.myfaq.android.screens.components.ErrorRetry
import app.myfaq.android.screens.components.LoadingIndicator
import app.myfaq.shared.api.dto.Category
import app.myfaq.shared.data.ActiveInstanceManager
import app.myfaq.shared.ui.CategoriesViewModel
import app.myfaq.shared.ui.UiState
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onCategoryClick: (categoryId: Int, categoryName: String) -> Unit,
    aim: ActiveInstanceManager = koinInject(),
) {
    val vm = remember { CategoriesViewModel(aim) }
    val state by vm.categories.collectAsState()

    LaunchedEffect(Unit) { vm.loadCategories() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Categories") })
        },
    ) { padding ->
        when (val s = state) {
            is UiState.Loading -> LoadingIndicator(modifier = Modifier.padding(padding))
            is UiState.Error ->
                ErrorRetry(
                    message = s.message,
                    onRetry = { vm.loadCategories() },
                    modifier = Modifier.padding(padding),
                )
            is UiState.Success -> {
                val sorted = remember(s.data) { buildFlatTree(s.data) }
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(sorted, key = { it.first.id }) { (category, depth) ->
                        CategoryRow(
                            category = category,
                            depth = depth,
                            onClick = { onCategoryClick(category.id, category.name) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    depth: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = (16 + depth * 24).dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Flattens a list of categories into a depth-ordered tree for display.
 * Returns pairs of (Category, depth).
 */
private fun buildFlatTree(categories: List<Category>): List<Pair<Category, Int>> {
    val byParent = categories.groupBy { it.parentId }
    val result = mutableListOf<Pair<Category, Int>>()

    fun walk(
        parentId: Int?,
        depth: Int,
    ) {
        byParent[parentId]?.sortedBy { it.name }?.forEach { cat ->
            result.add(cat to depth)
            walk(cat.id, depth + 1)
        }
    }

    // Root categories have parentId == null or 0
    walk(null, 0)
    walk(0, 0)

    // If tree-walk found nothing (flat list without parentIds), just list them
    if (result.isEmpty()) {
        categories.forEach { result.add(it to 0) }
    }

    return result
}
