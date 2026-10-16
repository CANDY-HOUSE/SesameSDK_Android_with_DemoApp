plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "co.candyhouse.sesame"
    compileSdk = rootProject.extra["compileSdkVersion"] as Int
    defaultConfig {
        minSdk = rootProject.extra["minSdkVersion"] as Int
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { buildConfig = true }
}

dependencies {
    api(libs.androidx.lifecycle.livedata.core)
    api(libs.kotlinx.coroutines.core)
    api(libs.gson)
    implementation(libs.androidx.annotation)
}
