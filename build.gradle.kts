// 根构建脚本：声明各模块用到的 plugin（版本由 settings + libs.versions.toml 管）
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.hilt) apply false
    // KSP（Room 注解处理器；版本跟随 Kotlin 2.0.20）
    id("com.google.devtools.ksp") version "2.0.20-1.0.25" apply false
}
