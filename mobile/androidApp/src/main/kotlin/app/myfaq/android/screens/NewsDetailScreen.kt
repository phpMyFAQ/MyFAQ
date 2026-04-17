package app.myfaq.android.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import app.myfaq.shared.api.dto.NewsItem
import app.myfaq.shared.data.ActiveInstanceManager
import app.myfaq.shared.ui.NewsDetailViewModel
import app.myfaq.shared.ui.UiState
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDetailScreen(
    newsId: Int,
    onBack: () -> Unit,
    aim: ActiveInstanceManager = koinInject(),
) {
    val vm = remember { NewsDetailViewModel(aim, newsId) }
    val state by vm.news.collectAsState()

    LaunchedEffect(newsId) { vm.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("News") },
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
            is UiState.Error -> ErrorRetry(
                message = s.message,
                onRetry = { vm.load() },
                modifier = Modifier.padding(padding),
            )
            is UiState.Success -> NewsDetailContent(
                news = s.data,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun NewsDetailContent(
    news: NewsItem,
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
        Text(
            text = news.header,
            style = MaterialTheme.typography.headlineSmall,
        )

        if (!news.date.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = news.date!!,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!news.authorName.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = news.authorName!!,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (news.content.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            val bgHex = String.format("#%06X", bgColor and 0xFFFFFF)
            val fgHex = String.format("#%06X", textColor and 0xFFFFFF)
            val htmlContent = buildNewsHtml(news.content, bgHex, fgHex)
            val density = LocalContext.current.resources.displayMetrics.density
            var webViewHeight by remember { mutableStateOf(200.dp) }

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        @SuppressLint("SetJavaScriptEnabled")
                        settings.javaScriptEnabled = true
                        setBackgroundColor(bgColor)
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                view?.evaluateJavascript("document.body.scrollHeight") { heightStr ->
                                    val heightPx = heightStr.toFloatOrNull() ?: return@evaluateJavascript
                                    webViewHeight = (heightPx / density).dp + 16.dp
                                }
                            }
                        }
                        loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    webView.setBackgroundColor(bgColor)
                    webView.loadDataWithBaseURL(null, buildNewsHtml(news.content, bgHex, fgHex), "text/html", "UTF-8", null)
                },
                modifier = Modifier.fillMaxWidth().height(webViewHeight),
            )
        }
    }
}

private fun buildNewsHtml(body: String, bgColor: String, fgColor: String): String =
    """
    <!DOCTYPE html><html><head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
      body { background-color: $bgColor; color: $fgColor;
             font-family: -apple-system, sans-serif; font-size: 16px;
             padding: 0; margin: 0; word-wrap: break-word; }
      img { max-width: 100%; height: auto; }
      a { color: #1a73e8; }
    </style>
    </head><body>$body</body></html>
    """.trimIndent()
