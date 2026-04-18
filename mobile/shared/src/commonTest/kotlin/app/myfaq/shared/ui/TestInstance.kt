package app.myfaq.shared.ui

import app.myfaq.shared.domain.AuthMode
import app.myfaq.shared.domain.Instance

/** Minimal [Instance] for ViewModel tests. */
internal fun testInstance(
    id: String = "instance-1",
    language: String = "en",
): Instance =
    Instance(
        id = id,
        displayName = "Test",
        baseUrl = "https://example.test",
        apiVersion = "4.0",
        language = language,
        authMode = AuthMode.NONE,
        lastSuccessfulPing = null,
        createdAt = 0L,
        updatedAt = 0L,
    )
