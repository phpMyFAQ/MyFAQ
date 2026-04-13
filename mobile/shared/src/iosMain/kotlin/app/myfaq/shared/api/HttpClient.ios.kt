@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package app.myfaq.shared.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.credentialForTrust
import platform.Foundation.serverTrust

actual fun createPlatformHttpClient(): HttpClient = HttpClient(Darwin) {
    install(ContentNegotiation) { json(HttpClientFactory.json) }

    engine {
        // DEV ONLY: trust all certificates (self-signed, localhost, etc.)
        // TODO: Gate behind a debug flag before release.
        handleChallenge { _, _, challenge, completionHandler ->
            if (challenge.protectionSpace.authenticationMethod ==
                NSURLAuthenticationMethodServerTrust
            ) {
                val trust = challenge.protectionSpace.serverTrust
                if (trust != null) {
                    val credential = NSURLCredential.credentialForTrust(trust)
                    completionHandler(
                        NSURLSessionAuthChallengeUseCredential,
                        credential
                    )
                    return@handleChallenge
                }
            }
            completionHandler(
                NSURLSessionAuthChallengePerformDefaultHandling,
                null
            )
        }
    }
}
