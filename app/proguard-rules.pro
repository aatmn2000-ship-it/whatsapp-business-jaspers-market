# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep kotlinx-serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.aatmn2000.aibuilder.core.**$$serializer { *; }
-keepclassmembers class com.aatmn2000.aibuilder.core.** {
    *** Companion;
}
-keepclasseswithmembers class com.aatmn2000.aibuilder.core.** {
    kotlinx.serialization.KSerializer serializer(...);
}
