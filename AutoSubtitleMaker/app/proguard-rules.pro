# --- Vosk + JNA (offline speech recognition) ---
# JNA binds native functions by reflection on class/method names, so nothing in these
# packages may be renamed or removed.
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
-keep class org.vosk.** { *; }
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn com.sun.jna.**

# --- kotlinx.serialization (project data + navigation routes) ---
-keepattributes *Annotation*, InnerClasses, Signature
-keepclassmembers @kotlinx.serialization.Serializable class com.verba.subtitles.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.verba.subtitles.** {
    kotlinx.serialization.KSerializer serializer(...);
}
