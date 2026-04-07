# Exchange Calc ProGuard Rules

# Keep Room entities
-keep class com.exchangecalc.app.data.** { *; }

# Keep Compose
-dontwarn androidx.compose.**

# Keep Billing
-keep class com.android.vending.billing.** { *; }

# Keep data classes
-keep class com.exchangecalc.app.model.** { *; }

# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.exchangecalc.app.**$$serializer { *; }
-keepclassmembers class com.exchangecalc.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.exchangecalc.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
