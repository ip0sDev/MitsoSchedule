import java.util.Base64

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
        versionCode = 4
        versionName = "1.14"

    }

    // Тот же ключ, что и у телефона (на CI он приходит из секретов). Без ключа, как при локальной
    // сборке, релиз часов подписывается отладочным ключом.
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
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseKeystore = signingConfigs.getByName("release").storeFile
            signingConfig = if (releaseKeystore != null && releaseKeystore.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Удален старый блок kotlinOptions
    // useLibrary("wear-sdk") // Removed this line
    testOptions {
        // android.util.Log (его используют и библиотеки) не должен падать в юнит-тестах
        unitTests.isReturnDefaultValues = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Новый способ указания версии JVM для Kotlin
kotlin {
    jvmToolchain(17)
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

    // Wear OS Tiles & Protolayout
    implementation(libs.androidx.wear.tiles)
    implementation(libs.androidx.wear.protolayout)
    implementation(libs.androidx.concurrent.futures.ktx)
    implementation(libs.guava)
    implementation(libs.androidx.wear.protolayout.material)
    implementation(libs.androidx.wear.protolayout.expression)

    // ----- НОВЫЕ ЗАВИСИМОСТИ -----
    // Wear Compose UI
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.compose.material) // ИЗМЕНЕНО: используем псевдоним 'androidx-compose-material' из toml, который ссылается на androidx.wear.compose:compose-material
    implementation(libs.androidx.compose.foundation) // ИЗМЕНЕНО: используем псевдоним 'androidx-compose-foundation' из toml, который ссылается на androidx.wear.compose:compose-foundation

    // Общий модуль: модели, сетевой слой (OkHttp), нормализация расписания
    implementation(project(":core"))

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json) // ДОБАВЛЕНА ЗАВИСИМОСТЬ

    // DataStore
    implementation(libs.androidx.datastore.preferences) // ADDED

    // ----- КОНЕЦ НОВЫХ ЗАВИСИМОСТЕЙ -----

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}