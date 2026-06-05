import java.util.Properties

plugins {
    id("com.android.application") version "9.2.1"
    id("com.google.gms.google-services") version "4.4.4"
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun apiKey(name: String): String {
    return listOf(
        localProperties.getProperty(name),
        providers.gradleProperty(name).orNull,
        System.getenv(name)
    ).firstOrNull { !it.isNullOrBlank() } ?: ""
}

fun buildConfigString(value: String): String {
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "FDC_API_KEY", buildConfigString(apiKey("FDC_API_KEY")))
        buildConfigField("String", "NUTRITIONIX_APP_ID", buildConfigString(apiKey("NUTRITIONIX_APP_ID")))
        buildConfigField("String", "NUTRITIONIX_APP_KEY", buildConfigString(apiKey("NUTRITIONIX_APP_KEY")))
        buildConfigField("String", "BARCODE_LOOKUP_API_KEY", buildConfigString(apiKey("BARCODE_LOOKUP_API_KEY")))
        buildConfigField("String", "UPCITEMDB_USER_KEY", buildConfigString(apiKey("UPCITEMDB_USER_KEY")))
        buildConfigField("String", "RETAILER_BACKEND_BASE_URL", buildConfigString(apiKey("RETAILER_BACKEND_BASE_URL")))
        buildConfigField("String", "BITWISE_LLM_BASE_URL", buildConfigString(apiKey("BITWISE_LLM_BASE_URL")))
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", buildConfigString(apiKey("GOOGLE_MAPS_API_KEY")))
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity:1.9.3")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    
    // Firebase
    implementation("com.google.firebase:firebase-auth:23.1.0")
    implementation("com.google.firebase:firebase-storage:21.0.1")
    implementation("com.google.firebase:firebase-functions:21.1.0")
    implementation("com.firebaseui:firebase-ui-auth:9.1.1")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")

    // Camera & ML Kit
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.1")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")

    // Utilities
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("androidx.test.espresso:espresso-idling-resource:3.6.1")
    // OpenAI SDK
    implementation("com.theokanning.openai-gpt3-java:service:0.18.2")

    // Web Scraping
    implementation("org.jsoup:jsoup:1.18.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
}
