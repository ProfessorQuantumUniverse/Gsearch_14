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

# --- WebView JS bridge is not used; no @JavascriptInterface keeps needed. ---
