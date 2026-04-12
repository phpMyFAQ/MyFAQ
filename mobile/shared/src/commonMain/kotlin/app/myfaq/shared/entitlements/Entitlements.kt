package app.myfaq.shared.entitlements

/**
 * Facade consulted by every Pro-gated UI action.
 *
 * Phase 0 ships a stub that always returns `false`. The real
 * StoreKit 2 and Play Billing implementations replace the platform
 * `actual` in Phase 3, without touching any calling site.
 */
object Entitlements {
    fun isPro(): Boolean = EntitlementsPlatform.isPro()
}

expect object EntitlementsPlatform {
    fun isPro(): Boolean
}
