package app.myfaq.shared.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Helper for collecting Kotlin StateFlow from Swift.
 * Returns a Job that can be cancelled to stop collection.
 *
 * Usage from Swift:
 *   let job = FlowCollectorKt.collectFlow(vm.results) { value in ... }
 *   job.cancel(cause: nil)
 */
fun <T> collectFlow(
    flow: StateFlow<T>,
    onEach: (T) -> Unit,
): Job {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    return scope.launch {
        flow.collect { value ->
            onEach(value)
        }
    }
}
