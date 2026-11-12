import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.player.streameraudio"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.player.streameraudio"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "2.2.0"
        val localProperties = Properties().apply {
            rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
        }
        val twitchClientId = (project.findProperty("TWITCH_CLIENT_ID") as String?)
            ?: System.getenv("TWITCH_CLIENT_ID")
            ?: localProperties.getProperty("TWITCH_CLIENT_ID")
            ?: ""
        buildConfigField("String", "TWITCH_CLIENT_ID", "\"$twitchClientId\"")
        buildConfigField("String", "TWITCH_REDIRECT_URL", "\"https://streameraudio-twitch-backend.netlify.app/oauth/callback\"")
        manifestPlaceholders["appLabel"] = "Streamer Audio"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val path = System.getenv("ANDROID_KEYSTORE_PATH")
            val password = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            val aliasValue = System.getenv("ANDROID_KEY_ALIAS")
            val keyPasswordValue = System.getenv("ANDROID_KEY_PASSWORD")
            if (!path.isNullOrBlank() && !password.isNullOrBlank() && !aliasValue.isNullOrBlank() && !keyPasswordValue.isNullOrBlank()) {
                storeFile = file(path)
                storePassword = password
                keyAlias = aliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["appLabel"] = "Streamer Audio Debug"
            buildConfigField("boolean", "USE_DEMO_CONTENT", "false")
        }
        create("screenshot") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".screenshots"
            versionNameSuffix = "-screenshots"
            manifestPlaceholders["appLabel"] = "Streamer Audio Screenshots"
            buildConfigField("boolean", "USE_DEMO_CONTENT", "true")
            matchingFallbacks += listOf("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("boolean", "USE_DEMO_CONTENT", "false")
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
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    testOptions.unitTests.isIncludeAndroidResources = true
    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
}

hilt { enableAggregatingTask = true }

baselineProfile {
    mergeIntoMain = true
    automaticGenerationDuringBuild = false
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.browser)
    implementation(libs.google.play.app.update)
    implementation(libs.google.play.app.update.ktx)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.session)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit.converter.scalars)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
}

tasks.configureEach {
    if (name == "bundleRelease" || name == "assembleRelease") {
        doFirst {
            val required = listOf("ANDROID_KEYSTORE_PATH", "ANDROID_KEYSTORE_PASSWORD", "ANDROID_KEY_ALIAS", "ANDROID_KEY_PASSWORD")
            val missing = required.filter { System.getenv(it).isNullOrBlank() }
            check(missing.isEmpty()) { "Release signing is not configured. Missing: ${missing.joinToString()}" }
        }
    }
}
