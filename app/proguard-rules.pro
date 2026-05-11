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

# Keep Firestore model classes - DO NOT OBFUSCATE OR REMOVE
# These classes require no-argument constructors for Firestore deserialization
-keep class com.example.kitchenbrain.model.Recipe { 
    public <init>();
    *;
}
-keep class com.example.kitchenbrain.model.Category { 
    public <init>();
    *;
}
-keep class com.example.kitchenbrain.model.FoodProduct { 
    public <init>();
    *;
}
-keep class com.example.kitchenbrain.model.NewsItem { 
    public <init>();
    *;
}
-keep class com.example.kitchenbrain.model.CulinaryNewsItem { 
    public <init>();
    *;
}
-keep class com.example.kitchenbrain.model.ChatMessage { 
    public <init>();
    *;
}
-keep class com.example.kitchenbrain.model.ChatUser { 
    public <init>();
    *;
}
-keep class com.example.kitchenbrain.User { 
    public <init>();
    *;
}

# Keep app classes
-keep class com.example.kitchenbrain.** { *; }
-keep class * extends java.lang.annotation.Annotation { *; }

# Kotlin coroutine runtime (state machines reference kotlin.coroutines.jvm.internal.* e.g. SpillingKt)
-keep class kotlin.coroutines.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**