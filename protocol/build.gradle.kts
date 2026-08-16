plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Pure-JVM, platform-independent implementation of the Jura Smart Connect ("BlueFrog")
// BLE protocol. Contains NO Android or CoreBluetooth dependencies so the exact same
// logic can be reasoned about, unit-tested on the JVM, and mirrored on macOS (Swift).
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
}
