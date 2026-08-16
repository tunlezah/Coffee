// Root build file. Plugin versions are declared per-module via the version catalog
// (gradle/libs.versions.toml) so that pure-JVM modules do not force resolution of the
// Android Gradle Plugin when the Android SDK is unavailable.

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
