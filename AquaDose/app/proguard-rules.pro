# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.aquadoselab.poolchemistry.**$$serializer { *; }
-keepclassmembers class com.aquadoselab.poolchemistry.** { *** Companion; }
-keepclasseswithmembers class com.aquadoselab.poolchemistry.** { kotlinx.serialization.KSerializer serializer(...); }
-keep class com.aquadoselab.poolchemistry.data.** { *; }

# Google Mobile Ads
-keep class com.google.android.gms.ads.** { *; }
