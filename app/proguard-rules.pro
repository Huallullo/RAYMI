# ============================================================
#  ProGuard / R8 rules para RAYMI
# ============================================================

# ── iText 7 ─────────────────────────────────────────────────
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# ── BouncyCastle (ligero, jdk18on) ──────────────────────────
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# ── Ktor HTTP Client ─────────────────────────────────────────
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ── Kotlinx Serialization ────────────────────────────────────
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}

# ── Firebase ─────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ── Hilt ─────────────────────────────────────────────────────
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# ── WorkManager ──────────────────────────────────────────────
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.hilt.work.HiltWorker { *; }

# ── Jetpack Compose ──────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── Kotlin Reflect ───────────────────────────────────────────
-keepclassmembers,allowshrinking,allowobfuscation class kotlin.reflect.jvm.internal.* {
    synthetic <methods>;
}
-keep class kotlin.reflect.jvm.internal.** { *; }

# ── XML / StAX (usado por algunas dependencias transitivas) ──
-keep class javax.xml.** { *; }
-dontwarn javax.xml.**
-keep class javax.xml.stream.** { *; }
-dontwarn javax.xml.stream.**
-keep class org.codehaus.stax2.** { *; }
-dontwarn org.codehaus.stax2.**

# ── Logging (SLF4J) ──────────────────────────────────────────
-dontwarn org.slf4j.**

# ── BND annotations (generado por R8) ────────────────────────
-dontwarn aQute.bnd.annotation.spi.ServiceProvider

# ── Coil ─────────────────────────────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**

# ── Modelos de dominio (evitar que R8 elimine campos) ────────
-keepclassmembers class com.raymi.app.domain.model.** { *; }
-keepclassmembers class com.raymi.app.data.model.dto.** { *; }
-keepclassmembers class com.raymi.app.data.remote.ReniecApiResponse { *; }
-keepclassmembers class com.raymi.app.data.remote.ReniecData { *; }