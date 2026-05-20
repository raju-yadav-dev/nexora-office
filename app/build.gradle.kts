plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.nexora.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nexora.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        setProperty("archivesBaseName", "nexora-office")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("release") {
            val storePath = providers.environmentVariable("NEXORA_RELEASE_STORE_FILE").orNull
            if (!storePath.isNullOrBlank()) {
                storeFile = file(storePath)
                storePassword = providers.environmentVariable("NEXORA_RELEASE_STORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("NEXORA_RELEASE_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("NEXORA_RELEASE_KEY_PASSWORD").orNull
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        release {
            val hasReleaseSigning = !providers.environmentVariable("NEXORA_RELEASE_STORE_FILE").orNull.isNullOrBlank()
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
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
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:filemanager"))
    implementation(project(":feature:editor"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
