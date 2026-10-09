plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.rebuild.local"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
}

dependencies {
    api(libs.room.runtime)
    api(libs.room.ktx)
    // Room 注解处理器（KSP，AGP8 下避免 room-compiler annotationProcessor 兼容问题）
    ksp("androidx.room:room-compiler:2.6.1")
    implementation(libs.androidx.core.ktx)
}
