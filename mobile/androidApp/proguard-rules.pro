# Phase 0: keep rules for the shared module and dependencies.
# R8 is enabled on release builds to catch keep-rule issues early
# (see phase-0-foundations.md).

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class app.myfaq.shared.**$$serializer { *; }
-keepclassmembers class app.myfaq.shared.** {
    *** Companion;
}
-keepclasseswithmembers class app.myfaq.shared.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# SQLCipher
-keep class net.zetetic.** { *; }

# Tink (transitively pulled in by androidx.security.crypto) references
# errorprone annotations only present at compile time on the Tink build.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
