# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisible*Annotations, EnclosingMethod
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.satya.calorietracker.**$$serializer { *; }
-keepclassmembers class com.satya.calorietracker.** {
    *** Companion;
}
-keepclasseswithmembers class com.satya.calorietracker.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Exceptions

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Glance widgets are instantiated by the framework
-keep class com.satya.calorietracker.widget.** { *; }
