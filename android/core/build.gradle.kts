import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
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
