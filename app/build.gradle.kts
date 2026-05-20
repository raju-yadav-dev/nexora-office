import com.android.build.api.dsl.ApplicationExtension

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
                ?: providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
            if (!storePath.isNullOrBlank()) {
                storeFile = file(storePath)
                storePassword = providers.environmentVariable("NEXORA_RELEASE_STORE_PASSWORD").orNull
                    ?: providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("NEXORA_RELEASE_KEY_ALIAS").orNull
                    ?: providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("NEXORA_RELEASE_KEY_PASSWORD").orNull
                    ?: providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        release {
            val hasReleaseSigning = !providers.environmentVariable("NEXORA_RELEASE_STORE_FILE").orNull.isNullOrBlank() ||
                !providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull.isNullOrBlank()
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

val releaseSigningConfigured = !providers.environmentVariable("NEXORA_RELEASE_STORE_FILE").orNull.isNullOrBlank() ||
    !providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull.isNullOrBlank()

gradle.taskGraph.whenReady {
    val needsReleaseSigning = allTasks.any { task ->
        task.name == "assembleRelease" || task.name == "bundleRelease" || task.name == "packageRelease"
    }
    if (needsReleaseSigning && !releaseSigningConfigured) {
        throw GradleException(
            "Release signing is not configured. Set NEXORA_RELEASE_* or ANDROID_* environment variables."
        )
    }
}

tasks.register("verifyReleaseApk") {
    group = "verification"
    description = "Verify release APK signing and zip alignment."
    dependsOn("assembleRelease")
    doLast {
        val apkDir = file("$buildDir/outputs/apk/release")
        val apkFile = apkDir.listFiles()?.firstOrNull { it.extension == "apk" }
            ?: throw GradleException("No release APK found in $apkDir")
        val androidExt = extensions.getByType<ApplicationExtension>()
        val sdkDir = androidExt.sdkDirectory.asFile.get()
        val buildToolsDir = sdkDir.resolve("build-tools")
            .listFiles()
            ?.maxByOrNull { it.name }
            ?: throw GradleException("No build-tools found under $sdkDir")

        val isWindows = System.getProperty("os.name").startsWith("Windows")
        val apksigner = buildToolsDir.resolve(if (isWindows) "apksigner.bat" else "apksigner")
        val zipalign = buildToolsDir.resolve(if (isWindows) "zipalign.exe" else "zipalign")

        if (!apksigner.exists()) {
            throw GradleException("apksigner not found at ${apksigner.absolutePath}")
        }
        if (!zipalign.exists()) {
            throw GradleException("zipalign not found at ${zipalign.absolutePath}")
        }

        exec {
            commandLine(apksigner.absolutePath, "verify", "--verbose", "--print-certs", apkFile.absolutePath)
        }
        exec {
            commandLine(zipalign.absolutePath, "-c", "-v", "4", apkFile.absolutePath)
        }
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
