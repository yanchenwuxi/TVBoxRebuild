pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        // 子模块用 id("com.google.devtools.ksp") 不带版本时，从这里解析
        "com.google.devtools.ksp" version "2.0.20-1.0.25"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TVBoxRebuild"
include(":app")
include(":core-player", ":core-spider", ":core-source",
        ":core-network", ":core-local", ":core-security")
include(":feature-home", ":feature-detail", ":feature-live", ":feature-settings")
include(":libs:quickjs")
