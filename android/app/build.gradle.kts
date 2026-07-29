import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.djs66256.short_drama"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.djs66256.short_drama"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        fun normalizeMallBaseUrl(rawBaseUrl: String): String {
            val trimmed = rawBaseUrl.trim().removeSuffix("/")
            return when {
                trimmed.endsWith("/api/v1") -> trimmed.removeSuffix("/api/v1")
                trimmed.endsWith("/api") -> trimmed.removeSuffix("/api")
                else -> trimmed
            }
        }

        val apiBaseUrl = localProperties.getProperty("api.base.url", "http://10.0.2.2:3000/api/")
        val mallBaseUrl = normalizeMallBaseUrl(
            localProperties.getProperty("mall.base.url", apiBaseUrl),
        )

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "MALL_BASE_URL", "\"$mallBaseUrl\"")
        buildConfigField("String", "APP_NAME", "\"ShortDrama\"")
        buildConfigField("String", "APP_VERSION", "\"${versionName}\"")
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("release-keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val properties = Properties()
                properties.load(keystorePropertiesFile.inputStream())
                storeFile = file(properties.getProperty("storeFile"))
                storePassword = properties.getProperty("storePassword")
                keyAlias = properties.getProperty("keyAlias")
                keyPassword = properties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val keystorePropertiesFile = rootProject.file("release-keystore.properties")
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Activity & Lifecycle
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.kotlinx.serialization.converter)

    // Core
    implementation(libs.core.ktx)

    // DataStore
    implementation(libs.datastore.preferences)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)

    // Compose UI Test
    androidTestImplementation(libs.compose.ui.test)
}

detekt {
    config.setFrom(file("$rootDir/.detekt/detekt.yml"))
    buildUponDefaultConfig = false
}
