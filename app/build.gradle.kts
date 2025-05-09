plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.hfad.linkup"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hfad.linkup"
        minSdk = 31
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation ("com.google.firebase:firebase-auth-ktx")
    implementation(libs.androidx.core.ktx)
    implementation ("com.github.bumptech.glide:glide:4.12.0")
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation ("com.google.firebase:firebase-database-ktx")
    implementation ("com.google.android.gms:play-services-auth:21.2.0")
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation ("androidx.recyclerview:recyclerview:1.2.1")
    implementation ("com.google.firebase:firebase-messaging:23.3.1")
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}