plugins { id("com.android.library") }

android {
    namespace = "com.rebuild.source"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
}

dependencies {
    implementation(libs.gson)
    implementation(libs.androidx.core.ktx)
    // 健康检测走 OkHttp（由 core-network 提供，这里仅声明）
    api(project(":core-network"))
}