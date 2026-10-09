plugins { id("com.android.library") }

android {
    namespace = "com.rebuild.player"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
}

dependencies {
    api(libs.media3.exoplayer)
    api(libs.media3.ui)
    api(libs.media3.datasource)
    implementation(libs.androidx.core.ktx)
}
