plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.sajiansehat"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rangga.sajiansehat"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // BuildConfig fields for API configuration
        buildConfigField("String", "API_BASE_URL", "\"https://sajisehat-api.wone.my.id/\"")
        buildConfigField("String", "API_KEY", "\"${getApiKey()}\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("../sajiansehat-release.keystore")
            storePassword = "sajiansehat123"
            keyAlias = "sajiansehat"
            keyPassword = "sajiansehat123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
    }
    
    lint {
        disable.add("UseAppTint")
    }
}

afterEvaluate {
    tasks.named("assembleRelease").configure {
        doLast {
            val releaseDir = layout.buildDirectory.dir("outputs/apk/release").get().asFile
            val srcFile = File(releaseDir, "app-release.apk")
            val destFile = File(releaseDir, "SajianSehat-v1.0.apk")
            if (srcFile.exists()) {
                srcFile.copyTo(destFile, overwrite = true)
            }
        }
    }
}

// Function untuk read API_KEY dari local.properties
fun getApiKey(): String {
    return try {
        val localPropsFile = File(rootDir, "local.properties")
        if (localPropsFile.exists()) {
            val lines = localPropsFile.readLines()
            lines.find { it.startsWith("API_KEY=") }?.substringAfter("=") ?: ""
        } else {
            ""
        }
    } catch (e: Exception) {
        ""
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    
    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    
    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}