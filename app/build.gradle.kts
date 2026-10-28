import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

val paypalClientId = localProperties.getProperty("PAYPAL_CLIENT_ID") ?: "BAAL5YDDEWDKSHd_mKoPC1baLgEjjyjLAg8YjFi0r2A1attsIww6Z7wDMoN0DtBIOwIqzZXC74fyGC6dZI"
val paypalSecret = localProperties.getProperty("PAYPAL_SECRET") ?: "EJ7q4Iw-J-9oy25zr2GNHWDTpFZGmJtRAWm-e1kvvLGO-hXtA7hI6j7Ir8l0p2l5KZA2Oz92bP_0_XXE"
val aiApiKey = localProperties.getProperty("AI_API_KEY") ?: "AQ.Ab8RN6LwSYty-i_phjbn7HeLUFRt7MTf2OehbN0y7u0Edrx7wg"

android {
    namespace = "com.example.hack"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.hack"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "PAYPAL_CLIENT_ID", "\"$paypalClientId\"")
        buildConfigField("String", "PAYPAL_SECRET", "\"$paypalSecret\"")
        buildConfigField("String", "AI_API_KEY", "\"$aiApiKey\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
