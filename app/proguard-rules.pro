# Keep kotlinx.serialization generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }

# Keep Room entities and DAOs
-keep class com.smartwardrobe.data.local.entity.** { *; }
-keep class com.smartwardrobe.data.local.dao.** { *; }

# Retrofit / OkHttp
-dontwarn retrofit2.**
-dontwarn okhttp3.**
