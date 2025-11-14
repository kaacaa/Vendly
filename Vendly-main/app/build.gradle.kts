plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.katarina.vendly"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.katarina.vendly"
        minSdk = 25
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
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
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.navigation.compose.android)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.benchmark.traceprocessor.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


// Firebase BOM (manages versions for all Firebase libs)
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))

// Firebase libraries
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-database-ktx")

    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.4.0")

    implementation("com.cloudinary:cloudinary-android:2.3.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("com.google.maps.android:maps-compose:4.4.1")
    implementation ("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")

    implementation("com.firebase:geofire-android-common:3.2.0")

    implementation ("com.google.firebase:firebase-messaging")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
}