import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val rootExtra = rootProject.extra

android {
    namespace = "co.candyhouse.app"
    compileSdk = rootExtra["compileSdkVersion"] as Int

    signingConfigs {
        create("release") {
            val passStore = System.getenv("storePassword")
            val passKey = System.getenv("keyPassword")
            val ssmKeyAlias = System.getenv("keyAlias")
            storeFile = file("keystore/Sesame.jks")
            storePassword = "$passStore"
            keyPassword = "$passKey"
            keyAlias = "$ssmKeyAlias"

            // 处理 CI 环境中的签名文件
            if (System.getenv("CI") == "true") {
                println("CI signing...")
                // CI 环境中跳过签名配置检查，因为签名将由 GitHub Actions 处理
                storeFile = null
                storePassword = null
                keyPassword = null
                keyAlias = null
            }
        }
    }

    defaultConfig {
        manifestPlaceholders["googleMapsApiKey"] = "${rootExtra["google.maps.apiKey"]}"
        val gitHash = providers.exec {
            commandLine("git", "rev-parse", "--short=9", "HEAD")
        }.standardOutput.asText.get().trim()
        applicationId = "co.candyhouse.sesame2"
        minSdk = rootExtra["minSdkVersion"] as Int
        targetSdk = rootExtra["targetSdkVersion"] as Int
        versionCode = 910
        versionName = "3.0.266"
        buildConfigField("String", "GIT_HASH", "\"$gitHash\"")
        // AWS Cognito 配置
        buildConfigField("String", "AWS_IDENTITY_POOL_ID", "\"${rootExtra["aws.cognito.identityPoolId"]}\"")
        buildConfigField("String", "AWS_USER_POOL_ID", "\"${rootExtra["aws.cognito.userPoolId"]}\"")
        buildConfigField("String", "AWS_APP_CLIENT_ID", "\"${rootExtra["aws.cognito.appClientId"]}\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "WEB_ORIGIN", "\"${rootExtra["candyhouse.sesame.web.dev"]}\"")
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "false"
            configure<CrashlyticsExtension> { mappingFileUploadEnabled = false }
            isMinifyEnabled = false
        }

        release {
            buildConfigField("String", "WEB_ORIGIN", "\"${rootExtra["candyhouse.sesame.web.prod"]}\"")
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "true"
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                println("Local signing...")
                signingConfig = releaseSigning
            }
            ndk {
                debugSymbolLevel = "FULL"
            }
        }

        create("ci") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
        }

    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/DEPENDENCIES"
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

androidComponents {
    onVariants(selector().withBuildType("ci")) { variant ->
        if (System.getenv("CI") == "true") {
            val versionCode = System.getenv("VERSION_CODE")
            if (versionCode != null) {
                variant.outputs.forEach { output ->
                    output.versionCode.set(versionCode.toInt())
                    println("Set CI version: $versionCode")
                }
            }
        }
    }
}

dependencies {
    implementation(project(":sesameSdk"))

    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.amplify.aws.auth.cognito)
    implementation(libs.amplify.core.kotlin)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.core)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.dfu)
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
}
