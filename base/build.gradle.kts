plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = "com.blockstream.base"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    /**  --- Modules ---------------------------------------------------------------------------- */
    api(project(":common"))
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Kotlin & KotlinX ------------------------------------------------------------------- */
    api(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.datetime)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Android / Google ------------------------------------------------------------------- */
    api(libs.androidx.core.ktx)
    api(libs.androidx.activity.ktx)
    api(libs.androidx.appcompat)
    api(libs.material)
    api(libs.androidx.constraintlayout)
    api(libs.androidx.swiperefreshlayout)
    api(libs.androidx.preference.ktx)
    api(libs.androidx.browser)
    api(libs.androidx.recyclerview)
    api(libs.androidx.viewpager2)
    api(libs.installreferrer)
    api(libs.androidx.startup.runtime)
    api(libs.compose.material3)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Lifecycle -------------------------------------------------------------------------- */
    api(libs.androidx.lifecycle.livedata.ktx)

    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.lifecycle.process)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Logging ---------------------------------------------------------------------------- */
    api(libs.slf4j.simple)
    api(libs.kotlin.logging.jvm)
    /** ----------------------------------------------------------------------------------------- */
}