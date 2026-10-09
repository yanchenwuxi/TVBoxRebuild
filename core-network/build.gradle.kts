plugins { id("com.android.library") }

android {
    namespace = "com.rebuild.net"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
}

dependencies {
    api(libs.okhttp)
    api(libs.okhttp.dnsoverhttps)
    api(libs.retrofit)
    api(libs.retrofit.gson)
    implementation(libs.gson)
    implementation(libs.androidx.core.ktx)
}
