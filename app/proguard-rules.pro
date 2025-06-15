# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class androidx.datastore.*.** {*;}

# ML Kit optimization rules for size reduction
-keep class com.google.mlkit.vision.text.** { *; }
-keep class com.google.android.gms.vision.** { *; }

# Allow aggressive optimization for ML Kit internal classes
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# Keep only essential ML Kit classes
-keep class com.google.mlkit.vision.text.TextRecognizer { *; }
-keep class com.google.mlkit.vision.text.Text { *; }
-keep class com.google.mlkit.vision.common.InputImage { *; }

# Aggressive optimization for release builds
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Remove unused ML Kit features to reduce size
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# Remove debug and verbose logging from all classes
-assumenosideeffects class * {
    void debug(...);
    void verbose(...);
}

# Optimize Kotlin metadata
-dontwarn kotlin.**
-dontwarn kotlinx.**
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# Remove unused resources and classes
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
