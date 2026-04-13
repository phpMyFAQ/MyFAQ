// Root build file. Phase 0 keeps configuration minimal — project-level
// plugin declarations with `apply false` so subprojects can alias them.

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

// Apply ktlint to all subprojects so `ktlintCheck` aggregates them
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.5.0")
        android.set(true)
        verbose.set(true)
        filter {
            exclude("**/build/**")
        }
    }
}

// Detekt: scan all source sets from root
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$rootDir/detekt.yml")
    source.setFrom(
        "shared/src/commonMain/kotlin",
        "shared/src/androidMain/kotlin",
        "shared/src/iosMain/kotlin",
        "androidApp/src/main/kotlin",
    )
}
