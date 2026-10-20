plugins { id("com.android.application") }
android {
    namespace = "app.msime.offline"
    compileSdk = 36
    defaultConfig {
        applicationId = "app.msime.offline"
        testInstrumentationRunner = "app.msime.offline.NopeDeviceSmoke"
        minSdk = 28
        targetSdk = 35
        versionCode = 15
        versionName = rootProject.file("VERSION").readText().trim() + "-offline"
        ndk { abiFilters += "arm64-v8a" }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets.getByName("main") {
        java.srcDirs("src/main/java", "src/engine/java", "src/layout/java", "src/skin/java", "src/features/java")
        assets.setSrcDirs(listOf("src/resources/assets"))
        res.setSrcDirs(listOf("src/resources/res"))
        jniLibs.setSrcDirs(listOf("src/engine/jniLibs"))
    }
    // 覆盖旧版时显式指定原开发证书，不依赖不同终端的默认用户目录。
    providers.environmentVariable("NOPE_DEBUG_KEYSTORE").orNull?.let { path ->
        signingConfigs.getByName("debug").storeFile = file(path)
    }
    packaging {
        jniLibs.useLegacyPackaging = false
        jniLibs.keepDebugSymbols += "**/*.so"
    }
    lint { abortOnError = true }
}
