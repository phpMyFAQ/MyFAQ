package app.myfaq.shared.entitlements

import kotlin.test.Test
import kotlin.test.assertFalse

class EntitlementsTest {

    @Test
    fun `Phase 0 stub never reports Pro`() {
        assertFalse(Entitlements.isPro())
        assertFalse(EntitlementsPlatform.isPro())
    }
}
