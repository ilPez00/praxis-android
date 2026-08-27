# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Gson + Retrofit: keep API models' fields (they are populated reflectively).
-keep class com.praxis.android.data.model.** { *; }
-keepattributes Signature
-keepattributes InnerClasses
-keep,allowobfuscation interface com.praxis.android.data.api.PraxisApi

# Tink (via androidx.security:security-crypto) references error-prone
# annotations that are compile-only; they are absent at runtime by design.
-dontwarn com.google.errorprone.annotations.**
