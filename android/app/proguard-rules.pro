# 短剧 App ProGuard 规则

# Kotlin
-keepattributes *Annotation*
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }

# Compose
-keep class androidx.compose.** { *; }

# 保持数据模型（序列化/反序列化）
-keep class com.djs66256.short_drama.** { *; }

# Retrofit / OkHttp（后续若接入）
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
