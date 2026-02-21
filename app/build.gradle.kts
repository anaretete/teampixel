import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

android {
    signingConfigs {
        getByName("debug") {
            storeFile = file("/Users/sameerasandakelum/Documents/Essentials-key.jks")
            storePassword = "202231"
            keyAlias = "key0"
            keyPassword = "202231"
        }
    }
    namespace = "com.sameerasw.pixsl"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.sameerasw.pixsl"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        signingConfig = signingConfigs.getByName("debug")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // Material3 expressive alpha for latest components
    implementation("androidx.compose.material3:material3:1.5.0-alpha12")

    // ViewModel for Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    // Coil for async image loading (avatars)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Supabase BOM + modules
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:gotrue-kt")
    implementation("io.github.jan-tennert.supabase:compose-auth")

    // Ktor engine for Supabase networking
    implementation("io.ktor:ktor-client-android:3.0.0")

    // Kotlin Serialization for JSON <-> data class conversion
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Nostr key generation
    implementation("com.github.cashapp:nostrino:0.0.1")

    // Material Icons Extended for UI icons
    implementation("androidx.compose.material:material-icons-extended:1.7.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}