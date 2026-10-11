# Room entities are accessed reflectively by generated code only; keep defaults.
# Google Mobile Ads, UMP and ML Kit ship their own consumer rules.

# Keep WorkManager workers (instantiated by class name).
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}
