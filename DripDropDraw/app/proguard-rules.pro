# Release-build (R8) rules.
# Keep line numbers so crash reports from release builds are readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# WorkManager (used inside the Google Mobile Ads SDK) creates its Room database by looking up
# the generated *_Impl class by name. Keep those classes and their constructors, or release
# builds crash at startup with "Failed to create an instance of androidx.work.impl.WorkDatabase".
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class androidx.work.impl.** { *; }
-keep class androidx.work.** extends androidx.work.ListenableWorker { <init>(...); }
-keep class androidx.startup.** { *; }
-keep class * implements androidx.startup.Initializer { <init>(); }
