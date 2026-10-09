plugins { id("com.android.library") }

android {
    namespace = "com.rebuild.spider"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
}

dependencies {
    // QuickJS 桥 + 内置解析
    api(project(":libs:quickjs"))
    // 网络层（内置 get/post/request）
    api(project(":core-network"))
    api(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.androidx.core.ktx)
}
