plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.praxis.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.praxis.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "SUPABASE_URL", "\"https://kuyzjjbeiawnnkkrjsda.supabase.co\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"sb_publishable_gI3zbQ0L6rt7IMe8XgW2UQ_Hzzxadtd\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"101857464392-ogonae71osimtc3c9qq1kpdq01mhde8b.apps.googleusercontent.com\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("praxis-release.keystore")
            storePassword = "praxis123"
            keyAlias = "praxis-key"
            keyPassword = "praxis123"
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:3001/api/\"")
            isDebuggable = false  // Disable debugging for speed
            packaging {
                resources {
                    excludes += "META-INF/DEPENDENCIES"
                    excludes += "META-INF/LICENSE"
                    excludes += "META-INF/LICENSE.txt"
                    excludes += "META-INF/NOTICE"
                    excludes += "META-INF/NOTICE.txt"
                }
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_BASE_URL", "\"https://web-production-646a4.up.railway.app/api/\"")
            packaging {
                resources {
                    excludes += "META-INF/DEPENDENCIES"
                    excludes += "META-INF/LICENSE"
                    excludes += "META-INF/LICENSE.txt"
                    excludes += "META-INF/NOTICE"
                    excludes += "META-INF/NOTICE.txt"
                }
            }
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xjvm-default=all"
        )
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("androidx.biometric:biometric:1.1.0")

    // Supabase
    implementation("io.github.jan-tennert.supabase:auth-kt:3.4.1")
    implementation("io.ktor:ktor-client-android:3.4.1")

     // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.appcompat:appcompat:1.7.1")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.04.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")  // Core only, not extended
    implementation("androidx.compose.material:material-icons-extended") {
        exclude(group = "androidx.compose.material", module = "material-icons-core")
    }  // Lazy load if needed

    // Material Components
    implementation("com.google.android.material:material:1.13.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Room Database (for local storage)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // Coroutines (ensure present)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Credential Manager + Google Sign-In (modern way)
    implementation("androidx.credentials:credentials:1.3.0-alpha01")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0-alpha01")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")

    // Constraint Layout
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    // WorkManager for background sync
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Health Connect for fitness data
    implementation("androidx.health.connect:connect-client:1.1.0-alpha09")

    // Google Play Services for Google APIs
    implementation("com.google.android.gms:play-services-fitness:21.1.0")
    implementation("com.google.api-client:google-api-client-android:2.2.0")
    implementation("com.google.apis:google-api-services-calendar:v3-rev411-1.25.0")
    implementation("com.google.http-client:google-http-client-gson:1.43.3")

    // Browser for OAuth custom tabs
    implementation("androidx.browser:browser:1.7.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.04.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
