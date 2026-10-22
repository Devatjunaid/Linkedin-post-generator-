# Google Mobile Ads and UMP ship their own consumer rules; WorkManager needs the extra rules below.
# Keep line numbers for readable crash reports in Play Console.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# WorkManager (pulled in by Google Mobile Ads) stores its jobs in a Room database.
# Room finds the generated "<Name>_Impl" class by name at runtime, so R8 must not
# rename or strip RoomDatabase subclasses or their constructors. Without this the
# release build crashes at launch with "Failed to create an instance of
# androidx.work.impl.WorkDatabase".
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class androidx.work.impl.WorkManagerInitializer { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
