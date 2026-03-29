plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
}

android {
  namespace = "com.mirrornode.app"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.mirrornode.app"
    minSdk = 30
    targetSdk = 36
    versionCode = 1
    versionName = "1.0.0"

    externalNativeBuild {
      cmake {
        cppFlags += "-std=c++20"
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
    }
  }

  buildFeatures {
    buildConfig = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = "17"
  }

  externalNativeBuild {
    cmake {
      path = file("src/main/cpp/CMakeLists.txt")
      version = "3.22.1"
    }
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

dependencies {
  implementation("androidx.core:core-ktx:1.17.0")
  implementation("androidx.appcompat:appcompat:1.7.1")
  implementation("androidx.activity:activity-ktx:1.12.2")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
  implementation("com.google.android.material:material:1.13.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
