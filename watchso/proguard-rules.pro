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
-keep class by.iposdev.watchso.data.** { *; }

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Jsoup
-keep class org.jsoup.** { *; }

# Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# Wear Compose
-keep class androidx.wear.compose.** { *; }