package app.myfaq.shared.data

import app.myfaq.shared.api.MyFaqApi
import app.myfaq.shared.api.MyFaqApiImpl
import app.myfaq.shared.api.createPlatformHttpClient
import app.myfaq.shared.domain.Instance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the currently active instance and lazily creates an
 * instance-scoped [MyFaqApi] and [FaqRepository] when the user
 * switches instances.
 */
class ActiveInstanceManager(
    private val cacheStore: CacheStore,
    private val apiFactory: (Instance) -> MyFaqApi = { instance ->
        MyFaqApiImpl(createPlatformHttpClient(), instance.baseUrl, instance.language)
    },
) {
    private val _activeInstance = MutableStateFlow<Instance?>(null)
    val activeInstance: StateFlow<Instance?> = _activeInstance.asStateFlow()

    private var _api: MyFaqApi? = null
    private var _repository: FaqRepository? = null

    val api: MyFaqApi get() = _api ?: error("No active instance. Call setActive() first.")
    val repository: FaqRepository get() = _repository ?: error("No active instance. Call setActive() first.")

    fun setActive(instance: Instance) {
        if (_activeInstance.value?.id == instance.id &&
            _activeInstance.value?.language == instance.language
        ) {
            return
        }
        _activeInstance.value = instance
        _api = apiFactory(instance)
        _repository = FaqRepository(_api!!, cacheStore, instance.id)
    }

    /**
     * Switches the active instance's content language. Recreates the API client
     * (Accept-Language header changes) and clears the per-instance cache so
     * cached responses in the previous language are not served.
     */
    fun setLanguage(language: String) {
        val current = _activeInstance.value ?: return
        if (current.language == language) return
        cacheStore.clearInstance(current.id)
        val updated = current.copy(language = language)
        _activeInstance.value = updated
        _api = apiFactory(updated)
        _repository = FaqRepository(_api!!, cacheStore, updated.id)
    }

    fun clear() {
        _activeInstance.value = null
        _api = null
        _repository = null
    }
}
