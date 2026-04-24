# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# ─── Performance Optimizations ──────────────────────────────────────────────────

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
-assumenosideeffects class kotlin.io.Console {
    public static void println(...);
}

# ─── Keep Data Classes ──────────────────────────────────────────────────────────
-keep class com.praxis.app.data.model.** { *; }
-keep class com.praxis.app.data.remote.** { *; }

# ─── Keep Compose ──────────────────────────────────────────────────────────────
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# ─── Keep Kotlin Coroutines ────────────────────────────────────────────────────
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ─── Keep Retrofit/OkHttp ──────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# ─── Keep Gson ─────────────────────────────────────────────────────────────────
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*

# ─── Keep Ktor/Supabase ───────────────────────────────────────────────────────
-dontwarn io.ktor.**
-dontwarn io.github.jan-tennert.supabase.**
-keep class io.ktor.** { *; }
-keep class io.github.jan-tennert.supabase.** { *; }

# ─── Keep WebView JS Interface ───────────────────────────────────────────────
-keep class com.praxis.app.WebAppInterface { *; }
-keepclassmembers class com.praxis.app.WebAppInterface { *; }

# ─── Remove unused resources ───────────────────────────────────────────────────
# R8 will automatically remove unused resources when shrinking is enabled
