plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.rebuild.home"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    buildFeatures { compose = true }
}

dependencies {
    // UI：Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling)
    implementation(libs.material3)
    // 数据/模型：Spider 的 Episode + 本地/源
    implementation(project(":core-spider"))
    implementation(project(":core-local"))
    implementation(libs.androidx.core.ktx)
    debugImplementation(libs.androidx.ui.tooling)
}
