import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(17)

    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)
            implementation(compose.runtime)
            implementation(compose.components.resources)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation("com.squareup.okhttp3:okhttp") { version { strictly("5.3.2") } }
            implementation(libs.kotlinx.coroutines.android)
        }
    }
}

android {
    namespace = "com.musicroom.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.musicroom.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1,INDEX.LIST,io.netty.versions.properties}" }
    }
    buildTypes {
        getByName("release") { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.resources {
    packageOfResClass = "com.musicroom.app.generated.resources"
}
