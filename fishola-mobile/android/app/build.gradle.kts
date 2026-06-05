import java.io.ByteArrayOutputStream
import java.util.Properties
import java.io.FileInputStream

interface InjectedExecOps {
    @get:Inject val execOps: ExecOperations
}

plugins {
    id("com.android.application")
}

val versCode: String by project
val versName: String by project
android {
    namespace = "fr.inrae.fishola"
    compileSdk = rootProject.extra.get("compileSdkVersion") as Int
    defaultConfig {
        applicationId = "fr.inrae.fishola"
        minSdk = rootProject.extra.get("minSdkVersion") as Int
        targetSdk = rootProject.extra.get("targetSdkVersion") as Int
        versionCode = versCode.toInt()
        versionName = versName
        testInstrumentationRunner = "androidx.test.runner.AndroixoJUnitRunner"
        resValue("string", "DEEP_LINK_URL", "fishola.demo.codelutin.com")
    }
    signingConfigs {
        create("signing") {
            val signinProps = Properties()
            signinProps.load(FileInputStream(file("./signin.properties")))
            storeFile = file(signinProps["RELEASE_STORE_FILE"]!!)
            keyAlias = signinProps["RELEASE_KEY_ALIAS"] as String
            storePassword = signinProps["RELEASE_STORE_PASSWORD"] as String
            keyPassword = signinProps["RELEASE_KEY_PASSWORD"] as String

            // Optional, specify signing versions used
            enableV1Signing = true
            enableV2Signing = true
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("signing")

            applicationVariants.configureEach {
                val versionName = this.versionName
                val versionCode = this.versionCode

                this.outputs
                    .map { it as com.android.build.gradle.internal.api.ApkVariantOutputImpl }
                    .forEach { output ->
                        val outputStream = ByteArrayOutputStream()
                        val injected = project.objects.newInstance<InjectedExecOps>()
                        injected.execOps.exec {
                            standardOutput = outputStream
                            commandLine("git", "rev-parse", "--short", "HEAD")
                        }
                        val gitRevision = outputStream.toString().trim()
                        output.outputFileName = "fishola-v${versionName}-c${versionCode}-r${gitRevision}.apk"
                    }
            }
        }
        getByName("debug") {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }
    flavorDimensions += "version"
    productFlavors {
        create("demo") {
            // Assigns this product flavor to the "version" flavor dimension.
            // If you are using only one dimension, this property is optional,
            // and the plugin automatically assigns all the module's flavors to
            // that dimension.
            dimension = "version"
            versionNameSuffix = "-demo"
            resValue("string", "DEEP_LINK_URL", "fishola.demo.codelutin.com")
        }
        create("beta") {
            dimension = "version"
            versionNameSuffix = "-beta"
            resValue("string", "DEEP_LINK_URL", "fishola-beta.demo.codelutin.com")
        }
        create("production") {
            dimension = "version"
            versionNameSuffix = ""
            resValue("string", "DEEP_LINK_URL", "api-fishola.inrae.fr")
        }
    }
}

repositories {
    flatDir{
        dirs("../capacitor-cordova-android-plugins/src/main/libs", "libs")
    }
}

dependencies {
    val androidxAppCompatVersion = rootProject.extra.get("androidxAppCompatVersion")
    val androidxCoordinatorLayoutVersion = rootProject.extra.get("androidxCoordinatorLayoutVersion")
    val coreSplashScreenVersion = rootProject.extra.get("coreSplashScreenVersion")
    val junitVersion = rootProject.extra.get("junitVersion")
    val androidxJunitVersion = rootProject.extra.get("androidxJunitVersion")
    val androidxEspressoCoreVersion = rootProject.extra.get("androidxEspressoCoreVersion")

    implementation("androidx.core:core-splashscreen:$coreSplashScreenVersion")
    implementation("androidx.coordinatorlayout:coordinatorlayout:$androidxCoordinatorLayoutVersion")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.appcompat:appcompat:$androidxAppCompatVersion")
    implementation(project(":capacitor-android"))
    testImplementation("junit:junit:$junitVersion")
    androidTestImplementation("androidx.test.ext:junit:$androidxJunitVersion")
    androidTestImplementation("androidx.test.espresso:espresso-core:$androidxEspressoCoreVersion")
    implementation(project(":capacitor-cordova-android-plugins"))
}

apply(from = "capacitor.build.gradle")

try {
    val servicesJSON = file("google-services.json")
    if (servicesJSON.readText().isNotEmpty()) {
        apply(plugin = "com.google.gms.google-services")
    }
} catch(e: Exception) {
    logger.warn("google-services.json not found, google-services plugin not applied. Push Notifications won't work")
}
