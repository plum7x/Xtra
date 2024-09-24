plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.navigation.safeargs)
}

kotlin {
    jvmToolchain(17)
}

android {
    signingConfigs {
        getByName("debug") {
            keyAlias = "debug"
            keyPassword = "123456"
            storeFile = file("debug-keystore.jks")
            storePassword = "123456"
        }
    }
    namespace = "com.github.andreyasadchy.xtra"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.github.andreyasadchy.xtra"
        minSdk = 16
        targetSdk = 35
        versionCode = 121
        versionName = "2.36.0"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            signingConfig = signingConfigs.getByName("debug")
            multiDexEnabled = true
        }
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    androidResources {
        generateLocaleConfig = true
    }
    lint {
        disable += "ContentDescription"
    }
    configurations.all {
        resolutionStrategy.force(listOf(
            "androidx.appcompat:appcompat:1.7.0-alpha03",
            "androidx.core:core-ktx:1.13.0-alpha01",
            "androidx.fragment:fragment-ktx:1.7.0-alpha06",
            "androidx.hilt:hilt-compiler:1.1.0",
            "androidx.hilt:hilt-work:1.1.0",
            "androidx.lifecycle:lifecycle-livedata:2.7.0-alpha03",
            "androidx.lifecycle:lifecycle-process:2.7.0-alpha03",
            "androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0-alpha03",
            "androidx.media3:media3-exoplayer:1.2.1",
            "androidx.media3:media3-exoplayer-hls:1.2.1",
            "androidx.media3:media3-session:1.2.1",
            "androidx.media3:media3-ui:1.2.1",
            "androidx.navigation:navigation-fragment:2.7.7",
            "androidx.navigation:navigation-ui:2.7.7",
            "androidx.paging:paging-runtime:3.3.0-alpha02",
            "androidx.webkit:webkit:1.9.0-alpha01",
            "androidx.work:work-runtime:2.9.1",
            "com.google.android.material:material:1.11.0",
            "com.squareup.okhttp3:okhttp:3.12.13",
            "com.squareup.okhttp3:okhttp-tls:3.12.13",
            "com.squareup.okhttp3:logging-interceptor:3.12.13",
            "com.squareup.retrofit2:retrofit:2.6.4",
        ))
    }
}

dependencies {
    implementation("org.conscrypt:conscrypt-android:2.5.2")
    implementation("androidx.multidex:multidex:2.0.1")

    //UI
    implementation(libs.material)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.preference.ktx)
    implementation(libs.swiperefreshlayout)
    implementation(libs.draglistview)

    //Architecture components
    implementation(libs.paging.runtime)
    //implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.process)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.room.paging)
    implementation(libs.work.runtime)
    implementation(libs.core.ktx)
    implementation(libs.fragment.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.webkit)

    //Misc
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.okhttp.tls)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.serialization)
    implementation(libs.serialization.json)
    implementation(libs.okio)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)
    implementation(libs.checker.qual)

    implementation(libs.glide)
    ksp(libs.glide.ksp)
    implementation(libs.glide.okhttp)
    implementation(libs.glide.webpdecoder)

    implementation(libs.hilt)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.work)
    ksp(libs.hilt.extension.compiler)

    implementation(libs.coroutines)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Delete large build log files from ~/.gradle/daemon/X.X/daemon-XXX.out.log
// Source: https://discuss.gradle.org/t/gradle-daemon-produces-a-lot-of-logs/9905
File("${project.gradle.gradleUserHomeDir.absolutePath}/daemon/${project.gradle.gradleVersion}").listFiles()?.forEach {
    if (it.name.endsWith(".out.log")) {
        // println("Deleting gradle log file: $it") // Optional debug output
        it.delete()
    }
}