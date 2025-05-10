import com.android.build.gradle.ProguardFiles.getDefaultProguardFile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}
android {
    namespace = "com.hfad.chattest1"
    compileSdk = 35 // Обновлено до версии 35

    defaultConfig {
        applicationId = "com.hfad.chattest1"
        minSdk = 31  // Рекомендую более низкую версию для большего охвата устройств
        targetSdk = 34  // Обновлено до версии 35
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
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
    implementation ("com.github.bumptech.glide:glide:4.12.0")
    implementation(libs.androidx.runner)
    implementation(libs.androidx.espresso.core)
    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    implementation ("com.squareup.okhttp3:okhttp:4.9.3")
    implementation ("org.json:json:20210307")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}


