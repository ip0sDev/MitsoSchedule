plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlinSerialization) // ИСПРАВЛЕННЫЙ ПСЕВДОНИМ ПЛАГИНА
}

android {
    namespace = "by.iposdev.watchso"
    compileSdk = 37

    defaultConfig {
        applicationId = "by.iposdev.watchso"
        minSdk = 34
        targetSdk = 37
        versionCode = 1
        versionName = "1.11"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // Удален старый блок kotlinOptions
    // useLibrary("wear-sdk") // Removed this line
    buildFeatures {
        compose = true
    }
}

// Новый способ указания версии JVM для Kotlin
kotlin {
    jvmToolchain(11)
}

dependencies {

    implementation(libs.play.services.wearable)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)

    // НЕ ЭТИ стандартные Compose foundation/material, они для мобильного приложения
    // implementation(libs.androidx.compose.material) // Стандартный Material
    // implementation(libs.androidx.compose.foundation) // Стандартный Foundation

    implementation(libs.androidx.wear.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.horologist.compose.tools)
    implementation(libs.androidx.watchface.complications.data.source.ktx)

    // ----- НОВЫЕ ЗАВИСИМОСТИ -----
    // Wear Compose UI
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.compose.material) // ИЗМЕНЕНО: используем псевдоним 'androidx-compose-material' из toml, который ссылается на androidx.wear.compose:compose-material
    implementation(libs.androidx.compose.foundation) // ИЗМЕНЕНО: используем псевдоним 'androidx-compose-foundation' из toml, который ссылается на androidx.wear.compose:compose-foundation

    // Network & Parsing
    // implementation(libs.okhttp) // ВРЕМЕННО ЗАКОММЕНТИРОВАНО
    implementation("com.squareup.okhttp3:okhttp:4.12.0") // ДОБАВЛЕНА ПРЯМАЯ ЗАВИСИМОСТЬ
    implementation("com.squareup.okhttp3:okhttp-urlconnection:4.12.0") // ДОБАВЛЕНА ЗАВИСИМОСТЬ ДЛЯ JavaNetCookieJar
    implementation(libs.jsoup)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json) // ДОБАВЛЕНА ЗАВИСИМОСТЬ

    // DataStore
    implementation(libs.androidx.datastore.preferences) // ADDED

    // ----- КОНЕЦ НОВЫХ ЗАВИСИМОСТЕЙ -----

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}