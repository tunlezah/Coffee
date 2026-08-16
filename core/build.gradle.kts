plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Platform-independent coffee domain logic ported 1:1 from the Swift app
// (Domain/ + RulesEngine/ + value-type Models/). No Android dependencies, so the
// espresso "science" is unit-tested on the JVM exactly as it was under XCTest.
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
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "failed", "skipped") }
}
