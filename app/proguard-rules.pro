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

# ========== REGLAS PARA LIBRERÍAS ==========

# iText PDF Library
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**
-keep class com.itextpdf.kernel.** { *; }
-keep class com.itextpdf.layout.** { *; }
-keep class com.itextpdf.io.** { *; }

# BouncyCastle (usado por iText)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Ktor HTTP Client
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Hilt
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# WorkManager
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Kotlin Serialization
-keepattributes Annotation, InnerClasses, EnclosingMethod, Signature
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeVisibleTypeAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation class kotlin.reflect.jvm.internal.* {
    synthetic <methods>;
}
-keep class kotlin.reflect.jvm.internal.** { *; }

# XML Processing (usado por algunas librerías)
-keep class javax.xml.** { *; }
-dontwarn javax.xml.**

# SLF4J (logging)
-dontwarn org.slf4j.**

# StAX XML
-keep class javax.xml.stream.** { *; }
-dontwarn javax.xml.stream.**
-keep class org.codehaus.stax2.** { *; }
-dontwarn org.codehaus.stax2.**
# BND Annotations (generado por R8)
-dontwarn aQute.bnd.annotation.spi.ServiceProvider
