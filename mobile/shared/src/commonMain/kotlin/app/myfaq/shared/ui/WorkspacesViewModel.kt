package app.myfaq.shared.ui

import app.myfaq.shared.api.MyFaqApiImpl
import app.myfaq.shared.api.createPlatformHttpClient
import app.myfaq.shared.api.dto.Meta
import app.myfaq.shared.data.ActiveInstanceManager
import app.myfaq.shared.data.MyFaqDatabase
import app.myfaq.shared.domain.AuthMode
import app.myfaq.shared.domain.Instance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class WorkspacesViewModel(
    private val db: MyFaqDatabase,
    private val activeInstanceManager: ActiveInstanceManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _instances = MutableStateFlow<List<Instance>>(emptyList())
    val instances: StateFlow<List<Instance>> = _instances.asStateFlow()

    private val _addState = MutableStateFlow<AddInstanceState>(AddInstanceState.Idle)
    val addState: StateFlow<AddInstanceState> = _addState.asStateFlow()

    init {
        refreshList()
    }

    fun refreshList() {
        val rows = db.instancesQueries.selectAll().executeAsList()
        _instances.value =
            rows.map { row ->
                Instance(
                    id = row.id,
                    displayName = row.display_name,
                    baseUrl = row.base_url,
                    apiVersion = row.api_version,
                    language = row.language,
                    authMode = AuthMode.valueOf(row.auth_mode),
                    lastSuccessfulPing = row.last_successful_ping,
                    createdAt = row.created_at,
                    updatedAt = row.updated_at,
                )
            }
    }

    fun probeInstance(url: String) {
        _addState.value = AddInstanceState.Probing
        scope.launch {
            try {
                val normalizedUrl = normalizeUrl(url)
                val client = createPlatformHttpClient()
                val api = MyFaqApiImpl(client, normalizedUrl)
                val meta = api.meta()
                _addState.value = AddInstanceState.Confirmed(normalizedUrl, meta)
            } catch (e: Exception) {
                _addState.value = AddInstanceState.Failed(e.message ?: "Connection failed")
            }
        }
    }

    fun confirmAdd(
        url: String,
        meta: Meta,
        apiToken: String? = null,
    ) {
        val now = Clock.System.now().epochSeconds
        val id = generateUuid()
        db.instancesQueries.insert(
            id = id,
            display_name = meta.title,
            base_url = url,
            api_version = "v4.0",
            language = meta.language,
            auth_mode = if (apiToken != null) AuthMode.TOKEN.name else AuthMode.NONE.name,
            last_successful_ping = now,
            created_at = now,
            updated_at = now,
        )
        refreshList()
        val instance = _instances.value.first { it.id == id }
        selectInstance(instance)
        _addState.value = AddInstanceState.Idle
    }

    fun selectInstance(instance: Instance) {
        activeInstanceManager.setActive(instance)
    }

    fun renameInstance(instanceId: String, newName: String) {
        val now = Clock.System.now().epochSeconds
        db.instancesQueries.updateDisplayName(newName, now, instanceId)
        refreshList()
    }

    fun deleteInstance(instanceId: String) {
        db.cacheEntriesQueries.deleteByInstance(instanceId)
        db.instancesQueries.deleteById(instanceId)
        if (activeInstanceManager.activeInstance.value?.id == instanceId) {
            activeInstanceManager.clear()
        }
        refreshList()
    }

    fun resetAddState() {
        _addState.value = AddInstanceState.Idle
    }

    private fun normalizeUrl(raw: String): String {
        var url = raw.trim().removeSuffix("/")
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            url = "https://$url"
        }
        return url
    }
}

sealed interface AddInstanceState {
    data object Idle : AddInstanceState

    data object Probing : AddInstanceState

    data class Confirmed(
        val url: String,
        val meta: Meta,
    ) : AddInstanceState

    data class Failed(
        val reason: String,
    ) : AddInstanceState
}

// Simple UUID generation for KMP
internal expect fun generateUuid(): String
