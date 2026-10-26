plugins {
    id("com.android.application")
}

android {
    namespace = "org.salt.savannah"
    compileSdk = 34
    defaultConfig {
        applicationId = "org.salt.savannah"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
}
