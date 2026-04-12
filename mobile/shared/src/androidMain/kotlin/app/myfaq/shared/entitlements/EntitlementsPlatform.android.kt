package app.myfaq.shared.entitlements

/**
 * Phase 0 stub. Always returns `false`. Phase 3 replaces this
 * implementation with a real Play Billing Library v7+ check against
 * cached + verified purchase state.
 */
actual object EntitlementsPlatform {
    actual fun isPro(): Boolean = false
}
