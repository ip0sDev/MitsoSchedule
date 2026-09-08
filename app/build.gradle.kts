import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlinSerialization)
}

// Новый способ указания версии JVM для Kotlin
kotlin {
    jvmToolchain(11)
}

android {
    namespace = "mitsoschedule.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "mitsoschedule.app"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val channelOverride = (project.findProperty("CHANNEL") as? String) ?: System.getenv("CHANNEL")
        if (!channelOverride.isNullOrBlank()) {
            buildConfigField("String", "CHANNEL", "\"$channelOverride\"")
        } else {
            buildConfigField("String", "CHANNEL", "\"release\"")
        }
    }

    signingConfigs {
        create("release") {
            val keystoreBase64 = (project.findProperty("KEYSTORE_BASE64") as? String)
                ?: System.getenv("KEYSTORE_BASE64")
            val keystoreFileProp = (project.findProperty("KEYSTORE_FILE") as? String)
                ?: System.getenv("KEYSTORE_FILE")

            if (!keystoreBase64.isNullOrBlank()) {
                val tempKeystore = file("${layout.buildDirectory.get()}/tmp/release.keystore")
                tempKeystore.parentFile.mkdirs()
                tempKeystore.writeBytes(Base64.getDecoder().decode(keystoreBase64.trim()))
                storeFile = tempKeystore
            } else if (!keystoreFileProp.isNullOrBlank()) {
                storeFile = file(keystoreFileProp)
            }

            storePassword = (project.findProperty("KEYSTORE_PASSWORD") as? String)
                ?: System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = (project.findProperty("KEY_ALIAS") as? String)
                ?: System.getenv("KEY_ALIAS") ?: ""
            keyPassword = (project.findProperty("KEY_PASSWORD") as? String)
                ?: System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val ksFile = signingConfigs.getByName("release").storeFile
            if (ksFile != null && ksFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.okhttp.urlconnection)
    // Jsoup
    implementation(libs.jsoup)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}