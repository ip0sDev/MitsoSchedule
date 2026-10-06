# ProGuard / R8 rules for Wear OS app

# Kotlinx Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    companion object;
}
-keep class kotlinx.serialization.** { *; }
# Модели расписания лежат в :core, их правила приходят из core/consumer-rules.pro

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# Wear Compose
-keep class androidx.wear.compose.** { *; }