import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    id("jacoco")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}
android {
    namespace = "com.raymi.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.raymi.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "1.1.0"
        manifestPlaceholders["usesCleartextTraffic"] = "false"
        manifestPlaceholders["ADMOB_APP_ID"] = project.findProperty("ADMOB_APP_ID") ?: "ca-app-pub-3940256099942544~3347511713"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // RENIEC API Configuration
        buildConfigField("String", "RENIEC_API_URL", "\"${project.findProperty("RENIEC_API_URL") ?: ""}\"")
        buildConfigField("String", "RENIEC_API_TOKEN", "\"${project.findProperty("RENIEC_API_TOKEN") ?: ""}\"")
        buildConfigField("String", "RENIEC_API_URL_FALLBACK", "\"${project.findProperty("RENIEC_API_URL_FALLBACK") ?: ""}\"")
        buildConfigField("String", "RENIEC_API_TOKEN_FALLBACK", "\"${project.findProperty("RENIEC_API_TOKEN_FALLBACK") ?: ""}\"")
        buildConfigField("String", "RENIEC_API_URL_FALLBACK2", "\"${project.findProperty("RENIEC_API_URL_FALLBACK2") ?: ""}\"")
        buildConfigField("String", "RENIEC_API_TOKEN_FALLBACK2", "\"${project.findProperty("RENIEC_API_TOKEN_FALLBACK2") ?: ""}\"")
        buildConfigField("String", "ADMOB_APP_ID", "\"${project.findProperty("ADMOB_APP_ID") ?: ""}\"")
        buildConfigField("String", "NUBEFACT_URL", "\"${project.findProperty("NUBEFACT_URL") ?: ""}\"")
        buildConfigField("String", "NUBEFACT_TOKEN", "\"${project.findProperty("NUBEFACT_TOKEN") ?: "6313dbbf2c5745f791d152579c6e3ad50d0e415693aa46cfad20a1e485fbc6bc"}\"")
        buildConfigField("String", "APIPERU_URL", "\"${project.findProperty("APIPERU_URL") ?: "https://apiperu.dev/api/cpe"}\"")
        buildConfigField("String", "APIPERU_TOKEN", "\"${project.findProperty("APIPERU_TOKEN") ?: "c8e569705a79f471d022671d85079d3731128154fc96cbd74bcb0c300709459c"}\"")
        buildConfigField("String", "MIAPI_URL", "\"${project.findProperty("MIAPI_URL") ?: "https://api.miapi.cloud/api/v1/cpe"}\"")
        buildConfigField("String", "MIAPI_TOKEN", "\"${project.findProperty("MIAPI_TOKEN") ?: "a05063ad-da54-4791-b556-6e49bcd2f93d"}\"")
        buildConfigField("String", "ADMOB_BANNER_ID", "\"${project.findProperty("ADMOB_BANNER_ID") ?: "ca-app-pub-3940256099942544/6300978111"}\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${project.findProperty("ADMOB_INTERSTITIAL_ID") ?: "ca-app-pub-3940256099942544/1033173712"}\"")
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }
    buildTypes {
        debug {
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }

        release {
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            signingConfig = signingConfigs.getByName("release")
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
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    kapt {
        correctErrorTypes = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
            excludes += "META-INF/*.kotlin_module"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

dependencies {
    testImplementation(libs.androidx.junit)
    // Core desugaring for iText compatibility
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Compose Navigation
    implementation(libs.androidx.navigation.compose)

    // Lifecycle ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.work.hilt)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    kapt(libs.androidx.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Splash Screen
    implementation(libs.androidx.core.splashscreen)

    // Google Ads (AdMob)
    implementation(libs.google.ads)

    // Material Components (Needed for XML themes)
    implementation(libs.material)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Coil
    implementation(libs.coil.compose)

    // Accompanist
    implementation(libs.accompanist.permissions)

    // Lottie
    implementation(libs.lottie.compose)

    // Barcode Scanning & Camera
    implementation(libs.barcode.scanning)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    
    // Explicitly add common for ML Kit if needed
    implementation(libs.mlkit.vision.common)
    implementation(libs.mlkit.barcode.scanning.common)

    // PDF - iText for Android
    implementation(libs.itext7.core) {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
        exclude(group = "org.bouncycastle", module = "bcpkix-jdk15on")
        exclude(group = "org.bouncycastle", module = "bcpkix-jdk18on")
    }
    implementation(libs.bouncycastle.bcprov)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // Google Play Billing
    implementation(libs.billing)

    // QR Generation
    implementation(libs.zxing.core)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.turbine)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))

    debugImplementation(libs.androidx.compose.ui.tooling)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Test configuration
tasks.withType<Test> {
    useJUnit()
}

// Jacoco coverage report
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
