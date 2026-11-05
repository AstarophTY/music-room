import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

/**
 * Values baked into the app at build time, read from the root .env file
 * (falls back to environment variables). Rebuild the app after changing them.
 */
val dotEnv: String = providers.fileContents(rootProject.layout.projectDirectory.file(".env")).asText.orElse("").get()
val envValue: (String) -> String = { key ->
    dotEnv.lineSequence().map { it.trim() }.firstOrNull { it.startsWith("$key=") }
        ?.substringAfter('=')?.trim()?.removeSurrounding("\"").orEmpty()
        .ifBlank { providers.environmentVariable(key).orNull.orEmpty() }
}
val backendUrl: String = envValue("BACKEND_URL").ifBlank { "http://10.0.2.2:8080" }
val googleClientId: String = envValue("GOOGLE_CLIENT_ID")

val generateAppEnv by tasks.registering {
    val values = mapOf("BACKEND_URL" to backendUrl, "GOOGLE_CLIENT_ID" to googleClientId)
    val outputDir = layout.buildDirectory.dir("generated/appEnv/kotlin")
    inputs.property("values", values)
    outputs.dir(outputDir)
    doLast {
        val constants = values.entries.joinToString("\n") { (key, value) ->
            val escaped = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$")
            "    const val $key = \"$escaped\""
        }
        val file = outputDir.get().file("com/musicroom/app/AppEnv.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package com.musicroom.app
            |
            |/** Generated from the root .env file by the generateAppEnv Gradle task. Do not edit. */
            |object AppEnv {
            |$constants
            |}
            |""".trimMargin(),
        )
    }
}

kotlin {
    jvmToolchain(17)

    android {
        namespace = "com.musicroom.app.compose"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources { enable = true }
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    // Desktop app (Windows, macOS, Linux): ./gradlew :composeApp:run
    jvm("desktop")

    // Web app: ./gradlew :composeApp:wasmJsBrowserDevelopmentRun, then http://localhost:3000
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("composeApp")
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                // 8080 is the API server.
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply { port = 3000 }
            }
        }
        binaries.executable()
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.configure { kotlin.srcDir(generateAppEnv) }
        val desktopMain by getting {
            dependencies {
                // `make windows` builds on Linux/WSL for Windows: -PdesktopTarget=windows picks the Windows natives.
                implementation(
                    if (providers.gradleProperty("desktopTarget").orNull == "windows") {
                        compose.desktop.windows_x64
                    } else {
                        compose.desktop.currentOs
                    },
                )
                implementation(libs.ktor.client.okhttp)
                implementation(libs.kotlinx.coroutines.swing)
                // Plays the songs uploaded to Music Room (MP3).
                implementation(libs.mp3spi.get().toString()) { exclude(group = "junit") }
            }
        }
        androidMain.dependencies {
            // Photo picker for the profile picture.
            implementation(libs.androidx.activity.compose)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.runtime)
            implementation(compose.components.resources)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.lucide.icons)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.qrose)
        }
    }
}

compose.resources {
    packageOfResClass = "com.musicroom.app.generated.resources"
}

compose.desktop {
    application {
        mainClass = "com.musicroom.app.MainKt"
        nativeDistributions {
            packageName = "Music Room"
            packageVersion = "1.0.0"
        }
    }
}

