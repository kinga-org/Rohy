plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Release signing comes from the environment (GitHub secrets in .github/workflows/release.yml), never from the
// repository. Without it, release builds are signed with the debug key: fine to try, not to distribute.
val releaseKeystore: String? = System.getenv("ROHY_KEYSTORE_PATH")

android {
    namespace = "app.rohy"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.rohy"
        minSdk = 29
        targetSdk = 36
        versionCode = (findProperty("rohyVersionCode") as String?)?.toInt() ?: 1
        versionName = findProperty("rohyVersion") as String? ?: "0.1.0"
    }

    signingConfigs {
        create("release") {
            if (releaseKeystore != null) {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("ROHY_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ROHY_KEY_ALIAS")
                keyPassword = System.getenv("ROHY_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName(if (releaseKeystore != null) "release" else "debug")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.code.scanner)
    testImplementation(libs.junit)
}
