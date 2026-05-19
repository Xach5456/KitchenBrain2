import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("kapt")
    id("com.google.gms.google-services")
}

// API KEYS: Resolve from local.properties
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun resolveApiKey(name: String): String {
    val fromGradle = project.findProperty(name)?.toString()?.trim().orEmpty()
    if (fromGradle.isNotEmpty()) return fromGradle
    return (localProps.getProperty(name) ?: "").trim()
}

val newsApiKeyResolved = resolveApiKey("NEWS_API_KEY")
val spoonacularApiKeyResolved = resolveApiKey("SPOONACULAR_API_KEY")
val grokApiKeyResolved = resolveApiKey("GROK_API_KEY")
val geminiApiKeyResolved = resolveApiKey("GEMINI_API_KEY")
val openaiApiKeyResolved = resolveApiKey("OPENAI_API_KEY")
val deepseekApiKeyResolved = resolveApiKey("DEEPSEEK_API_KEY")
val youtubeApiKeyResolved = resolveApiKey("YOUTUBE_API_KEY")
val cloudinaryApiKeyResolved = resolveApiKey("CLOUDINARY_API_KEY")

fun formatForBuildConfig(key: String) = key.replace("\\", "\\\\").replace("\"", "\\\"")

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:2.2.0")
        force("org.jetbrains.kotlin:kotlin-reflect:2.2.0")
    }
}

android {
    namespace = "com.example.kitchenbrain"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.kitchenbrain"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "NEWS_API_KEY", "\"${formatForBuildConfig(newsApiKeyResolved)}\"")
        buildConfigField("String", "SPOONACULAR_API_KEY", "\"${formatForBuildConfig(spoonacularApiKeyResolved)}\"")
        buildConfigField("String", "GROK_API_KEY", "\"${formatForBuildConfig(grokApiKeyResolved)}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${formatForBuildConfig(geminiApiKeyResolved)}\"")
        buildConfigField("String", "OPENAI_API_KEY", "\"${formatForBuildConfig(openaiApiKeyResolved)}\"")
        buildConfigField("String", "DEEPSEEK_API_KEY", "\"${formatForBuildConfig(deepseekApiKeyResolved)}\"")
        buildConfigField("String", "YOUTUBE_API_KEY", "\"${formatForBuildConfig(youtubeApiKeyResolved)}\"")
        buildConfigField("String", "CLOUDINARY_API_KEY", "\"${formatForBuildConfig(cloudinaryApiKeyResolved)}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.2.0"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    // Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation(libs.constraintlayout)
    
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    
    implementation(libs.glide)
    kapt(libs.glide.compiler)
    implementation(libs.circleimageview)
    implementation(libs.picasso)
    
    implementation(libs.okhttp)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation(libs.cloudinary.android)
    
    implementation(libs.swiperefreshlayout)
    implementation(libs.shimmer)
    
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    
    // ROOM
    val room_version = "2.7.0"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    implementation("androidx.room:room-paging:$room_version")
    kapt("androidx.room:room-compiler:$room_version")

    // PAGING
    implementation("androidx.paging:paging-runtime-ktx:3.3.0")
    
    // WORK MANAGER
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    
    implementation(libs.play.services.base)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

kapt {
    correctErrorTypes = true
}
