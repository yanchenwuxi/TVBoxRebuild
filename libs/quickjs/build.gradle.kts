plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.rebuild.quickjs"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
        // NDK 构建 libquickjs_rebuild.so（Bellard QuickJS + 我们的 JNI 包装）
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++11"
                arguments += listOf("-DANDROID_STL=c++_shared")
            }
        }
    }
    externalNativeBuild {
        cmake { path("src/main/jni/CMakeLists.txt") }
    }
    packaging {
        jniLibs { useLegacyPackaging = true }
        // 编出的 so 自动打到 APK 的 jniLibs/<abi>/
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.okhttp) // 内置方法（get/post）复用 OkHttp
}

// 集成步骤：
//   1) 跑 fetch-and-build.sh 把 Bellard QuickJS C 源码铺到 src/main/jni/quickjs/
//   2) 装 NDK + CMake（AS → SDK Manager）
//   3) ./gradlew :libs:quickjs:assembleRelease → 自动编出 libquickjs_rebuild.so
// 合规：不复用某商业闭源影音App libquickjs-android-wrapper.so；用 Bellard quickjs-ng（MIT）
