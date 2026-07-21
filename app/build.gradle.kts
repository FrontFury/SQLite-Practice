plugins {
    alias(libs.plugins.android.application)
    id("com.google.devtools.ksp")
}

android {
    buildFeatures {
        viewBinding = true
    }
    namespace = "com.example.student_directory"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.student_directory"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    val roomVersion = "2.8.4" // Use the latest stable version

    // Core Room Runtime components
    implementation("androidx.room:room-runtime:$roomVersion")

    // Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$roomVersion")

    // KSP code generator for Room annotations
    ksp("androidx.room:room-compiler:$roomVersion")

    val lifecycleVersion = "2.8.4" // Use the latest stable version

    // LiveData support
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")

    // Lifecycle utilities for Coroutines (provides lifecycleScope and repeatOnLifecycle)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")

    // ViewModel utilities (provides viewModelScope)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")

    implementation("io.coil-kt:coil:2.6.0")
}