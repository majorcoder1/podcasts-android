# R8 runs on release builds. Anything reached only by reflection has to be
# named here or it gets stripped and fails at runtime, not at build time.

# kotlinx.serialization generates a $$serializer for each @Serializable class
# and looks it up reflectively.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class live.fourthepeople.podcasts.**$$serializer { *; }
-keepclassmembers class live.fourthepeople.podcasts.** { *** Companion; }
-keepclasseswithmembers class live.fourthepeople.podcasts.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Enum names cross the DB boundary: DownloadState is stored as a string and read
# back with valueOf, so the constants must keep their names.
-keepclassmembers enum live.fourthepeople.podcasts.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Room entities are mapped by field name.
-keep class live.fourthepeople.podcasts.data.local.** { *; }

# Media3 uses reflection to pick a renderer/extractor set.
-dontwarn androidx.media3.**
-keep class androidx.media3.exoplayer.** { *; }

-dontwarn org.xmlpull.v1.**

# Keep line numbers so a stack trace from a user is readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
