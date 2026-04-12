package app.myfaq.shared.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Callback-shaped facade over [MyFaqApi.meta] so SwiftUI and Compose
 * can invoke it without touching coroutines directly.
 *
 * Phase 0 uses this from the placeholder screens on both platforms to
 * prove the shared module, generated client, and serialization are
 * all wired up end-to-end against a [io.ktor.client.engine.mock.MockEngine]
 * fixture.
 */
class MetaLoader(
    private val api: MyFaqApi,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    fun load(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        scope.launch {
            runCatching { api.meta() }
                .onSuccess { onSuccess("${it.title} — phpMyFAQ ${it.version}") }
                .onFailure { onError(it.message ?: it::class.simpleName ?: "unknown error") }
        }
    }
}
