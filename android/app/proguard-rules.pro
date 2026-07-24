# ShortDrama ProGuard 规则骨架

# Kotlin
-keepattributes *Annotation*
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }

# Compose
-keep class androidx.compose.** { *; }

# 保持数据模型（序列化/反序列化）
-keep class com.djs66256.short_drama.** { *; }

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.djs66256.short_drama.**$$serializer { *; }
-keepclassmembers class com.djs66256.short_drama.** {
    *** Companion;
}
-keepclasseswithmembers class com.djs66256.short_drama.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
