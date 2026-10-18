# ---- PdfBox-Android ----
# PdfBox loads fonts/CMaps from assets by name and uses some reflection internally.
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-keep class com.tom_roush.harmony.** { *; }
# Optional JPEG2000 decoder and BouncyCastle are not bundled (not needed for our features).
-dontwarn com.gemalto.jp2.**
-dontwarn org.bouncycastle.**
-dontwarn javax.xml.**
-dontwarn org.w3c.dom.**

# ---- Room ----
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# Google Mobile Ads and UMP ship their own consumer ProGuard rules.
