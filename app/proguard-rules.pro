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

# Supabase & Ktor rules to prevent over-minification of network/auth logic
-keep class io.ktor.** { *; }
-keep class io.github.jan.** { *; }

# Kotlin Serialization rules to preserve serializable fields and naming
-keepattributes *Annotation*, EnclosingMethod, InnerClasses
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}

# Keep project data models and specific serialization interfaces
-keep class com.sameerasw.pixsl.data.model.** { *; }
-keep class com.sameerasw.pixsl.data.Profile { *; }
-keep interface kotlinx.serialization.KSerializer { *; }