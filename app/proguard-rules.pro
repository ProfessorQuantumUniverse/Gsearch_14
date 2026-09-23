# Keep line numbers for readable crash reports, but hide the original file name.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- kotlinx.serialization ---
# The generated serializers are referenced reflectively via the companion object.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class **$serializer {
    *** INSTANCE;
}

# --- Glance app widgets ---
# Widget receivers are instantiated by the system from the manifest.
-keep class com.olafsapp.gsearch14.widget.** { *; }

# --- Room (via WorkManager, via Glance) ---
# Room creates the generated *_Impl database reflectively through its no-arg constructor.
# R8 full mode drops that constructor unless it is kept explicitly, which crashed 4.0 on
# every launch before any app code ran.
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# --- WebView JS bridge is not used; no @JavascriptInterface keeps needed. ---
