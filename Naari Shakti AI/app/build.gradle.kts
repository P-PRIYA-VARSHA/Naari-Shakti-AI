plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Add Firebase plugins
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    // Add Kapt for Room
    id("kotlin-kapt")
    // Add Hilt
    id("dagger.hilt.android.plugin")
    // Add Parcelize
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.secureshe"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.secureshe"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Increase startup timeout to prevent ANR
        manifestPlaceholders["android:allowBackup"] = "true"
        // Worker Authorization secret used for Cloudflare Worker proxy
        buildConfigField("String", "WORKER_AUTH", "\"y+U7shEKHgzTZmUNfMDz58LVb/wF2aVQjg20rkTkg2Y=\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    // Fix 16KB page size compatibility
    ndkVersion = "25.2.9519653"
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    // Fix 16KB page size compatibility and exclude duplicate META-INF resources
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt"
            )
        }
    }

    // Include external Probono sources and resources
    sourceSets {
        getByName("main") {
            // Use absolute paths via rootProject for reliability
            java.srcDir(file("${rootProject.projectDir}/external/Probono/ShaktiBotProBono/app/src/main/java"))
            // Do NOT merge external res to avoid duplicate resources
            assets.srcDir(file("${rootProject.projectDir}/external/Probono/ShaktiBotProBono/app/src/main/assets"))
        }
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // Compose Google Fonts (for Humanist sans-serif like Source Sans 3)
    implementation("androidx.compose.ui:ui-text-google-fonts")
    // AppCompat + Material (needed for AI Legal Tools UI)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Firebase Authentication
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    // AI Legal Tools uses Analytics + Realtime Database
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    // Google Sign-In for external Probono flow
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.firebase:firebase-functions")
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")
    
    // DataStore for local storage
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Hilt for dependency injection
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Maps and location
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.maps.android:maps-compose:4.4.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    // Removed Google Places SDK (using OSM search instead)

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // HTTP client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Retrofit for AdminUploadApi
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.11.0")
    implementation("com.squareup.moshi:moshi:1.15.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    
    // Icons
    implementation("androidx.compose.material:material-icons-extended:1.6.3")
    
    // Phase 2 Dependencies
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // OkHttp for HTTP requests
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // JSON handling
    implementation("org.json:json:20231013")
    
    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    
    // Location Services (single version)
    
    // Phone contacts
    implementation("androidx.activity:activity-ktx:1.9.0")
    
    // CameraX
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-video:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    
    // Biometric Authentication
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    
    // Encryption for PIN storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ML Kit - Face Detection used by external Probono verification
    implementation("com.google.mlkit:face-detection:16.1.5")

    // Google Drive API client libraries used by external Probono flow
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.api-client:google-api-client-android:2.5.0")
    implementation("com.google.http-client:google-http-client-android:1.43.3")
    implementation("com.google.http-client:google-http-client-gson:1.43.3")
    implementation("com.google.apis:google-api-services-drive:v3-rev20230815-2.0.0")
    
    // Pin gRPC to avoid LoadBalancer NoSuchMethodError due to transitive mismatches
    implementation("io.grpc:grpc-okhttp:1.62.2")
    implementation("io.grpc:grpc-protobuf-lite:1.62.2")
    implementation("io.grpc:grpc-stub:1.62.2")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}