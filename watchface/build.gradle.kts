plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.tanqueo.watchface"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.tanqueo.watchface"
        minSdk = 33
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }
}
