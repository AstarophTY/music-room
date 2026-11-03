rootProject.name = "music-room"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Downloads the JDK 17 toolchain automatically when it is not installed.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
    }
}

include(":shared")
include(":server")

val localProps = java.util.Properties().apply {
    val f = file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val androidSdk: String? = localProps.getProperty("sdk.dir")
    ?: System.getenv("ANDROID_HOME")
    ?: System.getenv("ANDROID_SDK_ROOT")

if (androidSdk != null && file(androidSdk).exists()) {
    include(":composeApp", ":androidApp")
} else {
    logger.lifecycle("[music-room] No Android SDK found: skipping :composeApp and :androidApp modules.")
}
