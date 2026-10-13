# kotlinx.serialization (backup file format)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.devatjunaid.renevo.**$$serializer { *; }
-keepclassmembers class com.devatjunaid.renevo.** { *** Companion; }
-keepclasseswithmembers class com.devatjunaid.renevo.** { kotlinx.serialization.KSerializer serializer(...); }

# WorkManager workers are created by reflection
-keep class * extends androidx.work.ListenableWorker { <init>(android.content.Context, androidx.work.WorkerParameters); }
