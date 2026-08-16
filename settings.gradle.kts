pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "cawfee-android"

// Pure-JVM modules: the platform-independent coffee domain logic (:core) and the
// shared Jura BLE protocol (:protocol). These build & test without the Android SDK.
include(":core")
include(":protocol")

// The Android application module requires the Android SDK. It is included only when
// an SDK is available, so :core and :protocol can be compiled and unit-tested on
// machines / CI jobs that do not have the Android SDK installed.
val hasAndroidSdk = System.getenv("ANDROID_HOME") != null ||
    System.getenv("ANDROID_SDK_ROOT") != null ||
    file("local.properties").exists()
if (hasAndroidSdk) {
    include(":app")
} else {
    logger.lifecycle("[settings] Android SDK not detected — :app module excluded. Building :core and :protocol only.")
}
