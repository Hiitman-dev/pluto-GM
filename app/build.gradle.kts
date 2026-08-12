import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.pluto.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pluto.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        // ── Secrets (loaded from local.properties, never committed) ──
        // To set up: add these lines to your local.properties file:
        //   PLUTO_API_KEY=your_api_key_here
        //   PLUTO_API_BASE_URL=https://your-api-server.com
        //   PLUTO_FALLBACK_SERVER_1=https://fallback1.com
        //   PLUTO_FALLBACK_SERVER_2=https://fallback2.com
        val localProps = Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localProps.load(FileInputStream(localPropsFile))
        }
        buildConfigField(
            "String",
            "API_KEY",
            "\"${localProps.getProperty("PLUTO_API_KEY", "")}\""
        )
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"${localProps.getProperty("PLUTO_API_BASE_URL", "https://server-hi-speed-iran.info")}\""
        )
        buildConfigField(
            "String",
            "FALLBACK_SERVER_1",
            "\"${localProps.getProperty("PLUTO_FALLBACK_SERVER_1", "https://hostinnegar.com")}\""
        )
        buildConfigField(
            "String",
            "FALLBACK_SERVER_2",
            "\"${localProps.getProperty("PLUTO_FALLBACK_SERVER_2", "https://windowsdiba.info")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Signing: configure via key.properties (NOT committed)
            val keystorePropsFile = rootProject.file("key.properties")
            val keystoreProps = Properties()
            if (keystorePropsFile.exists()) {
                keystoreProps.load(FileInputStream(keystorePropsFile))
                val storeFilePath = keystoreProps.getProperty("storeFile") ?: ""
                if (file(storeFilePath).exists()) {
                    signingConfig = signingConfigs.create("release").apply {
                        keyAlias = keystoreProps.getProperty("keyAlias")
                        keyPassword = keystoreProps.getProperty("keyPassword")
                        storeFile = file(storeFilePath)
                        storePassword = keystoreProps.getProperty("storePassword")
                    }
                }
            }
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core modules
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:media"))
    implementation(project(":core:navigation"))
    implementation(project(":core:notifications"))
    implementation(project(":core:download"))

    // Feature modules
    implementation(project(":feature:splash"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:home"))
    implementation(project(":feature:search"))
    implementation(project(":feature:details"))
    implementation(project(":feature:player"))
    implementation(project(":feature:downloads"))
    implementation(project(":feature:favorites"))
    implementation(project(":feature:history"))
    implementation(project(":feature:notifications"))
    implementation(project(":feature:settings"))

    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    debugImplementation(libs.androidx.ui.tooling)

    // Navigation + Hilt
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.androidx.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Accompanist
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.permissions)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
