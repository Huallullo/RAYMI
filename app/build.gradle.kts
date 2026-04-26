plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    id("jacoco")
}

android {
    namespace = "com.raymi.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.raymi.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "RENIEC_API_URL", "\"${project.findProperty("RENIEC_API_URL")}\"")
        buildConfigField("String", "RENIEC_API_TOKEN", "\"${project.findProperty("RENIEC_API_TOKEN")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Requerido por iText para usar java.time en minSdk < 26
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            // iText trae varias licencias y manifests que entran en conflicto
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/*.kotlin_module"
        }
    }
}

dependencies {
    // ── Core desugaring (necesario para iText en Android < API 26) ──────────
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")

    // ── AndroidX Core ───────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // ── Compose BOM ─────────────────────────────────────────────────────────
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // ── Compose Navigation ──────────────────────────────────────────────────
    implementation(libs.androidx.navigation.compose)

    // ── Lifecycle ViewModel ─────────────────────────────────────────────────
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // ── Firebase ────────────────────────────────────────────────────────────
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)

    // ── WorkManager ─────────────────────────────────────────────────────────
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.work.hilt)

    // ── Hilt ────────────────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // ── Coroutines ──────────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // ── Splash Screen ───────────────────────────────────────────────────────
    implementation(libs.androidx.core.splashscreen)

    // ── DataStore ───────────────────────────────────────────────────────────
    implementation(libs.androidx.datastore.preferences)

    // ── Coil ────────────────────────────────────────────────────────────────
    implementation(libs.coil.compose)

    // ── Accompanist ─────────────────────────────────────────────────────────
    implementation(libs.accompanist.permissions)

    // ── Lottie ──────────────────────────────────────────────────────────────
    implementation(libs.lottie.compose)

    // ── PDF – iText para Android ────────────────────────────────────────────
    // NOTA: iText7-core 8.x requiere desugaring (isCoreLibraryDesugaringEnabled = true).
    // Si el tamaño del APK es crítico, considera usar AndroidPdfDocument (nativo) o
    // la librería "com.itextpdf:itext7-core:7.2.5" que tiene menos dependencias.
    implementation("com.itextpdf:itext7-core:7.2.6") {
        // BouncyCastle completo es muy pesado en Android; excluir módulos no necesarios.
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
        exclude(group = "org.bouncycastle", module = "bcpkix-jdk15on")
    }
    // Versión ligera de BouncyCastle compatible con Android
    implementation("org.bouncycastle:bcprov-jdk18on:1.77")

    // ── Ktor HTTP client (para ReniecService) ───────────────────────────────
    implementation("io.ktor:ktor-client-android:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    implementation("io.ktor:ktor-client-logging:2.3.12")
    implementation("io.ktor:ktor-client-plugins:2.3.12")

    // ── Kotlin Serialization (requerida por Ktor) ───────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // ────────────────────────── PRUEBAS ────────────────────────────────────
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.turbine)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

// ── JUnit para tests unitarios ──────────────────────────────────────────────
tasks.withType<Test> {
    useJUnit()
}

// ── Reporte de cobertura Jacoco ─────────────────────────────────────────────
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    sourceDirectories.setFrom(files("src/main/java"))
    classDirectories.setFrom(
        fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
            exclude(
                "**/R.class",
                "**/R$*.class",
                "**/BuildConfig.*",
                "**/Manifest*.*",
                "**/*_HiltModules*",
                "**/*_Factory*",
                "**/*_MembersInjector*"
            )
        }
    )
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) {
            include("jacoco/testDebugUnitTest.exec")
        }
    )
}