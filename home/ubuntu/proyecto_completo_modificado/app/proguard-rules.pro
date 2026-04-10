# ── Doey Release ProGuard Rules ──────────────────────────────────────────────

# Keep application class
-keep class com.doey.DoeyApplication { *; }

# Keep all Activities, Services, Receivers, Providers
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.accessibilityservice.AccessibilityService
-keep public class * extends android.service.notification.NotificationListenerService

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep Compose - don't obfuscate composable functions
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Keep Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-keep class kotlinx.coroutines.android.** { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep JSON parsing (org.json is built-in, no rules needed)
# Keep kotlinx.serialization if used
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep Picovoice Porcupine
-keep class ai.picovoice.** { *; }
-dontwarn ai.picovoice.**

# Keep ExoPlayer / Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Google Play Services Location
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.**

# Keep AndroidX Security (EncryptedSharedPreferences)
-keep class androidx.security.crypto.** { *; }

# Keep DataStore
-keep class androidx.datastore.** { *; }

# Keep reflection targets used in accessibility service
-keepclassmembers class com.doey.services.DoeyAccessibilityService {
    public *;
}

# Keep all service/agent classes fully accessible
-keep class com.doey.agent.** { *; }
-keep class com.doey.llm.** { *; }
-keep class com.doey.tools.** { *; }
-keep class com.doey.services.** { *; }

# Preserve line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

# Suppress warnings for missing classes from optional deps
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
