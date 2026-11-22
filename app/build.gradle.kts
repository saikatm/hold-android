plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.saikat.hold"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.saikat.hold"
        minSdk = 29
        targetSdk = 36
        versionCode = 2
        versionName = "1.5"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

base {
    archivesName.set("Hold-1.5")
}

dependencies {
    implementation("com.google.android.material:material:1.12.0")
}
