plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.lsplugin.resopt)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.parcelize)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
}

val sourceRevision = providers.exec {
    workingDir(rootProject.projectDir)
    commandLine("git", "rev-parse", "--short=8", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText.get().trim().takeIf { it.matches(Regex("[0-9a-f]{8}")) } ?: "local"

android {
    namespace = "moe.chenxy.hyperpods"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hxlh50k.pods"
        minSdk = 35
        targetSdk = 36
        versionCode = 18
        versionName = "0.1.0-alpha.18"
        resourceConfigurations += listOf("en", "zh-rCN")
        buildConfigField("String", "BUILD_REVISION", "\"$sourceRevision\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags += ""
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("String", "BUILD_INSTANCE", "\"$sourceRevision-${System.currentTimeMillis()}\"")
            signingConfig = signingConfigs.getByName("debug")
            // Keep test APKs installable without HyperOS's debuggable-app warning.
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            buildConfigField("String", "BUILD_INSTANCE", "\"$sourceRevision\"")
            // A release must never inherit the development signing key.
            signingConfig = null
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            multiDexEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    dependenciesInfo.includeInApk = false

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(JavaVersion.VERSION_22.majorVersion)
        }
    }

    kotlin {
        jvmToolchain(JavaVersion.VERSION_22.majorVersion.toInt())
    }

    buildFeatures {
        buildConfig = true
    }

    // Wear-state tests exercise scheduling only, not Android service behavior.
    testOptions.unitTests.isReturnDefaultValues = true

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/**.version"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            excludes += "okhttp3/**"
            excludes += "kotlin/**"
            excludes += "org/**"
            excludes += "**.properties"
            excludes += "**.bin"
            excludes += "kotlin-tooling-metadata.json"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

configurations.configureEach {
//    exclude(group = "androidx.appcompat", module = "appcompat")
    exclude(group = "androidx.lifecycle", module = "lifecycle-viewmodel-ktx")
}

dependencies {
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.9.4")
    // 3.7.0 avoids InputManager.getInstance reflection on API 23+, including Android 17.
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("org.mockito:mockito-core:5.20.0")
    implementation(libs.coreKtx)
    implementation(libs.appcompat)
    compileOnly(libs.xposedApi)
    implementation(libs.kotlinx.serialization.json)
    // UI Kits
    implementation(libs.yukonga.miuix)
    implementation(libs.androidx.activity.compose)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.ui)
    implementation(compose.components.resources)
    implementation(compose.preview)
    debugImplementation(compose.uiTooling)
    implementation(libs.haze)
    implementation(libs.hyperfocusapi)
    implementation(libs.gaze.capsule)
    implementation("org.jetbrains.kotlin:kotlin-reflect:${libs.versions.kotlin.get()}")
}
