import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    // No kotlin-android plugin: AGP 9 compiles Kotlin itself (built-in Kotlin).
}

// Firebase: only applied when google-services.json exists, so the app still builds without it.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Backend URL. Default = Android emulator -> your computer. Override in android/local.properties:
//   MMM_BASE_URL=http://192.168.1.50:8000/api/      (physical phone over Wi-Fi)
//   MMM_BASE_URL=http://localhost:8000/api/          (physical phone with: adb reverse tcp:8000 tcp:8000)
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val mmmBaseUrl: String = (localProperties.getProperty("MMM_BASE_URL") ?: "http://10.0.2.2:8000/api/")
    .trim()
    .let { if (it.endsWith("/")) it else "$it/" } // Retrofit requires a trailing slash

android {
    namespace = "com.maumela.magnummanagement"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.maumela.magnummanagement"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "MMM_BASE_URL", "\"$mmmBaseUrl\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            // android.util.Log etc. return defaults instead of crashing in JVM unit tests
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Compose + Material 3
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // REST client (required external library): Retrofit + OkHttp + kotlinx.serialization
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Local storage: Room cache (read-through) + DataStore (preferences, encrypted token)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    // Images from URLs, and charts for the Energy module (second external library)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.mpandroidchart)

    // Firebase Cloud Messaging (required SDK)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    // Instrumented / tooling
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}