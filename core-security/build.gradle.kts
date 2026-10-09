plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.rebuild.security"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.androidx.core.ktx)
    // 本地持久化加密（SQLCipher 可选；MVP 先标占位）
    // api("net.zetetic:sqlcipher-android:4.5.4")
}
