# KumaLift ProGuard / R8 Rules

# Keep Kotlin Serialization models & serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowshrinking class com.bearbones.kumalift.domain.model.** { *; }

# Keep SQLDelight database & queries
-keep class com.bearbones.kumalift.database.** { *; }
-keep class app.cash.sqldelight.** { *; }

# Health Connect SDK & Permissions
-keep class androidx.health.connect.client.** { *; }

# Coil image loading & decoders
-dontwarn coil.**
-keep class coil.** { *; }

# Android Keystore & Security
-keepclassmembers class * extends java.security.KeyStore { *; }
