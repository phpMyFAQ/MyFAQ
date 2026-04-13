package app.myfaq.android.screens

import android.content.Intent
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.myfaq.android.screens.components.ErrorRetry
import app.myfaq.android.screens.components.LoadingIndicator
import app.myfaq.shared.api.dto.Comment
import app.myfaq.shared.api.dto.FaqDetail
import app.myfaq.shared.data.ActiveInstanceManager
import app.myfaq.shared.ui.FaqDetailViewModel
import app.myfaq.shared.ui.UiState
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqDetailScreen(
    categoryId: Int,
    faqId: Int,
    onBack: () -> Unit,
    onPaywall: () -> Unit,
    aim: ActiveInstanceManager = koinInject(),
) {
    val vm = remember { FaqDetailViewModel(aim) }
    val faqState by vm.faq.collectAsState()
    val commentsState by vm.comments.collectAsState()

    LaunchedEffect(categoryId, faqId) { vm.load(categoryId, faqId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FAQ") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (faqState is UiState.Success) {
                        val context = LocalContext.current
                        val faq = (faqState as UiState.Success<FaqDetail>).data
                        IconButton(onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, faq.question)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (val s = faqState) {
            is UiState.Loading -> LoadingIndicator(modifier = Modifier.padding(padding))
            is UiState.Error -> ErrorRetry(
                message = s.message,
                onRetry = { vm.load(categoryId, faqId) },
                modifier = Modifier.padding(padding),
            )
            is UiState.Success -> {
                FaqDetailContent(
                    faq = s.data,
                    commentsState = commentsState,
                    onPaywall = onPaywall,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FaqDetailContent(
    faq: FaqDetail,
    commentsState: UiState<List<Comment>>,
    onPaywall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = MaterialTheme.colorScheme.surface.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Question heading
        Text(
            text = faq.question,
            style = MaterialTheme.typography.headlineSmall,
        )

        if (!faq.author.isNullOrBlank() || !faq.updated.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = listOfNotNull(faq.author, faq.updated).joinToString(" | "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // HTML answer in WebView
        if (faq.answer.isNotBlank()) {
            val bgHex = String.format("#%06X", bgColor and 0xFFFFFF)
            val fgHex = String.format("#%06X", textColor and 0xFFFFFF)
            val htmlContent = buildHtml(faq.answer, bgHex, fgHex)

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = false
                        setBackgroundColor(bgColor)
                        loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    webView.setBackgroundColor(bgColor)
                    val content = buildHtml(faq.answer, bgHex, fgHex)
                    webView.loadDataWithBaseURL(null, content, "text/html", "UTF-8", null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
            )
        }

        // Tags
        if (faq.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                faq.tags.forEach { tag ->
                    AssistChip(
                        onClick = {},
                        label = { Text(tag) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Rate button (paywall)
        OutlinedButton(
            onClick = onPaywall,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Star, contentDescription = null)
            Text("  Rate this FAQ", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()

        // Comments section
        CommentsSection(commentsState)
    }
}

@Composable
private fun CommentsSection(state: UiState<List<Comment>>) {
    var expanded by remember { mutableStateOf(false) }

    when (state) {
        is UiState.Loading -> {
            Text(
                "Loading comments...",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        is UiState.Error -> {
            Text(
                "Could not load comments",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        is UiState.Success -> {
            if (state.data.isEmpty()) {
                Text(
                    "No comments yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        if (expanded) "Hide comments (${state.data.size})"
                        else "Show comments (${state.data.size})",
                    )
                }
                AnimatedVisibility(visible = expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.data.forEach { comment ->
                            CommentCard(comment)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentCard(comment: Comment) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = comment.author,
                    style = MaterialTheme.typography.labelMedium,
                )
                if (!comment.created.isNullOrBlank()) {
                    Text(
                        text = comment.created,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = comment.body,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun buildHtml(body: String, bgColor: String, fgColor: String): String =
    """
    <!DOCTYPE html>
    <html>
    <head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
      body {
        background-color: $bgColor;
        color: $fgColor;
        font-family: -apple-system, sans-serif;
        font-size: 16px;
        padding: 0;
        margin: 0;
        word-wrap: break-word;
      }
      img { max-width: 100%; height: auto; }
      a { color: #1a73e8; }
      pre, code { overflow-x: auto; }
    </style>
    </head>
    <body>$body</body>
    </html>
    """.trimIndent()
