import java.util.Properties

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ksp) apply false
}

// Build Version
extra["compileSdkVersion"] = 36
extra["minSdkVersion"] = 24
extra["targetSdkVersion"] = 36

// Load Config
val configFile = file("app.properties")
if (configFile.exists()) {
    val props = Properties()
    configFile.inputStream().use { props.load(it) }

    props.forEach { key, value ->
        extra[key.toString()] = value
    }
}
