plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.beingmechon.stand"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.beingmechon.stand"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    // "lite" has no notification listener. Google Play Protect blocks the install of
    // "full" from a browser in some countries, because of that listener.
    flavorDimensions += "kind"
    productFlavors {
        create("lite") { dimension = "kind" }
        create("full") { dimension = "kind" }
    }

    signingConfigs {
        // A fixed key, so that a new build can update the installed app.
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

kotlin {
    jvmToolchain(17)
}
