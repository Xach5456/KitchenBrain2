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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Rules to handle potential hidden API usage
-dontwarn android.view.**
-dontwarn android.graphics.**
-dontwarn android.opengl.**
-keep class android.view.** { *; }
-keep class android.graphics.** { *; }
-keep class android.opengl.** { *; }

# Keep Firebase related classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep app classes
-keep class com.example.kitchenbrain.** { *; }
-keep class * extends java.lang.annotation.Annotation { *; }