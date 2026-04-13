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
    private val db: MyFaqDatabase,
    private val cacheStore: CacheStore,
) {
    private val _activeInstance = MutableStateFlow<Instance?>(null)
    val activeInstance: StateFlow<Instance?> = _activeInstance.asStateFlow()

    private var _api: MyFaqApi? = null
    private var _repository: FaqRepository? = null

    val api: MyFaqApi get() = _api ?: error("No active instance. Call setActive() first.")
    val repository: FaqRepository get() = _repository ?: error("No active instance. Call setActive() first.")

    fun setActive(instance: Instance) {
        if (_activeInstance.value?.id == instance.id) return
        _activeInstance.value = instance
        val client = createPlatformHttpClient()
        _api = MyFaqApiImpl(client, instance.baseUrl)
        _repository = FaqRepository(_api!!, cacheStore, instance.id)
    }

    fun clear() {
        _activeInstance.value = null
        _api = null
        _repository = null
    }
}
