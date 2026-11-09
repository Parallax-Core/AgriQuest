import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Leaf Detector server settings. Override them in local.properties (not committed):
//   agriquest.aiServerUrl=https://agriquest-ai-server.onrender.com
//   agriquest.aiApiKey=<only if API_KEY is set on Render>
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val aiServerUrl: String = providers.environmentVariable("AI_SERVER_URL").orNull
    ?: localProperties.getProperty("agriquest.aiServerUrl", "https://agriquest-ai-server.onrender.com")
val aiApiKey: String = providers.environmentVariable("AI_API_KEY").orNull ?: localProperties.getProperty("agriquest.aiApiKey", "")
// Supabase public client configuration lives in untracked local.properties.
val supabaseUrl: String = providers.environmentVariable("SUPABASE_URL").orNull ?: localProperties.getProperty("agriquest.supabaseUrl", "")
val supabaseAnonKey: String = providers.environmentVariable("SUPABASE_ANON_KEY").orNull ?: localProperties.getProperty("agriquest.supabaseAnonKey", "")
val releaseKeystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""

android {
    namespace = "com.agriquest"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.agriquest"
        minSdk = 24
        targetSdk = 37
        versionCode = providers.environmentVariable("AGRIQUEST_VERSION_CODE").orNull?.toInt() ?: 1
        versionName = providers.environmentVariable("AGRIQUEST_VERSION_NAME").orNull ?: "1.0"

        buildConfigField("String", "AI_SERVER_URL", quoted(aiServerUrl))
        buildConfigField("String", "AI_API_KEY", quoted(aiApiKey))
        buildConfigField("String", "SUPABASE_URL", quoted(supabaseUrl))
        buildConfigField("String", "SUPABASE_ANON_KEY", quoted(supabaseAnonKey))
    }

    if (releaseKeystorePath != null) {
        signingConfigs.create("release") {
            storeFile = file(releaseKeystorePath)
            storePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").get()
            keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").get()
            keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").get()
            storeType = "JKS"
        }
    }

    buildTypes {
        release {
            if (releaseKeystorePath != null) signingConfig = signingConfigs.getByName("release")
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
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.material)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
