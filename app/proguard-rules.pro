# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keepclassmembers class * { @dagger.hilt.android.lifecycle.HiltViewModel *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.pluto.**$$serializer { *; }
-keepclassmembers class com.pluto.** {
    *** Companion;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# OkHttp / Retrofit
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes Exceptions

# App
-keep class com.pluto.app.BuildConfig { *; }
