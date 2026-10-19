plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Cada build no GitHub recebe um número crescente (1, 2, 3...).
// Ele vira o versionCode, usado pelo app para saber se há atualização.
val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "com.example.iptvplayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.iptvplayer"
        minSdk = 24
        targetSdk = 35
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
        vectorDrawables { useSupportLibrary = true }
    }

    // Chave fixa: todas as versões têm a mesma assinatura, então o Android
    // aceita instalar a atualização por cima sem desinstalar o app.
    signingConfigs {
        create("app") {
            storeFile = file("keystore/iptvplayer.jks")
            storePassword = "iptvplayer"
            keyAlias = "iptvplayer"
            keyPassword = "iptvplayer"
        }
    }

    // Duas versões do app, instaláveis lado a lado no mesmo aparelho
    flavorDimensions += "cor"
    productFlavors {
        create("verde") {
            dimension = "cor"
            buildConfigField("String", "THEME_COLOR", "\"verde\"")
            resValue("string", "app_name", "IPTV Player")
        }
        create("rosa") {
            dimension = "cor"
            applicationIdSuffix = ".rosa"
            versionNameSuffix = "-rosa"
            buildConfigField("String", "THEME_COLOR", "\"rosa\"")
            resValue("string", "app_name", "IPTV Player Rosa")
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("app")
        }
        release {
            signingConfig = signingConfigs.getByName("app")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    // ---------- AndroidX / Compose ----------
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ---------- Persistência local ----------
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ---------- Rede (Retrofit + OkHttp + Gson) ----------
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // ---------- Imagens (logos / capas) ----------
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")

    // ---------- Player: Media3 (ExoPlayer) ----------
    val media3 = "1.5.1"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-exoplayer-hls:$media3")
    implementation("androidx.media3:media3-ui:$media3")
    implementation("androidx.media3:media3-datasource-okhttp:$media3")
}
