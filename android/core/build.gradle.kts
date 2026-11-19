import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
    // `core` is plain Kotlin compiled against a recent JDK: it could call Java APIs Android only gained later
    // (URLDecoder.decode(String, Charset) is Android 13+ and crashed pairing on Android 10). Animal Sniffer
    // checks every call against the Android 10 API (the app's minSdk) and fails `check` otherwise.
    alias(libs.plugins.animalsniffer)
}

animalsniffer {
    sourceSets = listOf(project.sourceSets["main"])
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    signature("${libs.android.api.signature.get()}@signature")
    api(libs.okhttp)
    implementation(libs.bouncycastle)
    implementation(libs.serialization.json)
    testImplementation(libs.junit)
}

tasks.test {
    systemProperty("rohy.vectors", rootProject.file("../protocol/test-vectors.json").absolutePath)
    System.getenv("ROHY_HARNESS")?.let { environment("ROHY_HARNESS", it) }
}

kover {
    reports {
        verify {
            rule {
                minBound(80, CoverageUnit.LINE)
                minBound(80, CoverageUnit.BRANCH)
                minBound(80, CoverageUnit.INSTRUCTION)
            }
        }
    }
}
