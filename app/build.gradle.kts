import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

val dotenv = Properties().apply {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        load(envFile.inputStream())
    }
}

fun env(key: String, defaultValue: String = ""): String =
    "\"${dotenv.getProperty(key, defaultValue)}\""

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.compose.compiler)
    id("com.google.devtools.ksp")
}

android {
    val versionCodeFile = File("versionCode.txt")
    val incrementFile = File("increment.txt")
    val increment = !(if (incrementFile.exists()) incrementFile.readText().isBlank() else true)
    val fileVersionCode = (if (versionCodeFile.exists()) versionCodeFile.readText() else "0").toInt().let {
        if (increment) it + 1 else it
    }

    versionCodeFile.createNewFile()
    incrementFile.createNewFile()
    versionCodeFile.writeText(fileVersionCode.toString())
    incrementFile.writeText("")
    namespace = "id.my.nanclouder.nanhistory"
    compileSdk = 35

    defaultConfig {
        applicationId = "id.my.nanclouder.nanhistory"
        minSdk = 31
        targetSdk = 35
        versionCode = fileVersionCode
        versionName = "2.0.2"

        val now = ZonedDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX")
        val buildTime = now.format(formatter)

        buildConfigField("String", "ENCRYPTION_KEY", env("ENCRYPTION_KEY", "6VaEaqHzAzptmbdLYFWF5fnWFoBr4Raf"))
        buildConfigField("String", "LEGACY_ENCRYPTION_KEY", env("LEGACY_ENCRYPTION_KEY", "gWn88Iezr1ZwPqxLYkQ3Zv2w4uTI0Eu5"))

        buildConfigField(
            "String",
            "BUILD_TIME",
            "\"$buildTime\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            versionNameSuffix = "r"
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            versionNameSuffix = "d"
            applicationIdSuffix = ".dev"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    lint {
        baseline = file("lint-baseline.xml")
    }
}

//composeCompiler {
//    reportsDestination = layout.buildDirectory.dir("compose_compiler")
//    stabilityConfigurationFile = rootProject.layout.projectDirectory.file("stability_config.conf")
//}

dependencies {
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation(libs.capturable)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.foundation)
    // implementation(libs.kotlin.stdlib)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)
    // implementation(libs.ffmpeg.kit.full.v602)
    implementation(libs.compose.colorpicker)
    implementation(libs.android.sdk.v1151)
    implementation(libs.google.accompanist.permissions)
    implementation(libs.androidx.activity)
    implementation(libs.osmdroid.android)
    implementation(libs.gson)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.play.services.location)
    implementation(libs.androidx.material3.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.material.icons.extended)
}