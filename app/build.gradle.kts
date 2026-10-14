plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// =====================================================================
//  ADMOB IDs  --  YAHAN APNI ASLI IDs LIKHNI HAIN
//  (Jab tak XXXX likha hai, app Google ke TEST ads use karega)
//
//  1) ADMOB_APP_ID    -> AdMob > Apps > App settings mein milta hai
//                        format: ca-app-pub-<16 digits>~<10 digits>
//  2) ADMOB_BANNER_ID -> AdMob > Ad units > Banner ka Ad unit ID
//                        format: ca-app-pub-<16 digits>/<10 digits>
// =====================================================================
val ADMOB_APP_ID = "ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX"      // <-- App ID yahan
val ADMOB_BANNER_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"   // <-- Banner Ad Unit ID yahan

// Google ke official test IDs (placeholder ke time par auto use hoti hain)
val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"

val effectiveAppId = if (ADMOB_APP_ID.contains("XXXX")) TEST_APP_ID else ADMOB_APP_ID
val effectiveBannerId = if (ADMOB_BANNER_ID.contains("XXXX")) TEST_BANNER_ID else ADMOB_BANNER_ID

android {
    namespace = "com.watermark.videoapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.watermark.videoapp"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        manifestPlaceholders["admobAppId"] = effectiveAppId
        buildConfigField("String", "ADMOB_BANNER_ID", "\"$effectiveBannerId\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    lint {
        abortOnError = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // Offline video processing (Google ka Media3 Transformer)
    implementation("androidx.media3:media3-common:1.4.1")
    implementation("androidx.media3:media3-effect:1.4.1")
    implementation("androidx.media3:media3-transformer:1.4.1")

    // Banner ads
    implementation("com.google.android.gms:play-services-ads:23.3.0")
}
