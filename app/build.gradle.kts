plugins { id("com.android.application") }

android {
    namespace = "com.rebuild.tvbox"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.rebuild.tvbox"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.material3)

    implementation(project(":core-player"))
    implementation(project(":core-spider"))
    implementation(project(":core-source"))
    implementation(project(":core-network"))
    implementation(project(":core-local"))
    implementation(project(":feature-home"))
    implementation(project(":feature-detail"))
    implementation(project(":feature-settings"))

    implementation(libs.androidx.coil)
    implementation(libs.sentry)
}
