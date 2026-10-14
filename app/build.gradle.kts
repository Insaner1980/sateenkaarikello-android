import io.gitlab.arturbosch.detekt.Detekt
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.owasp.dependency.check)
    alias(libs.plugins.stability.analyzer)
}

android {
    namespace = "fi.sateenkaarikello"
    compileSdk = 37

    defaultConfig {
        applicationId = "fi.sateenkaarikello"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Julkaisuavain ja salasanat ovat repositorion ulkopuolella, esim. ~/.gradle/gradle.properties:
    // SATEENKAARI_KEYSTORE, SATEENKAARI_KEYSTORE_PASSWORD, SATEENKAARI_KEY_ALIAS, SATEENKAARI_KEY_PASSWORD.
    val keystorePath = providers.gradleProperty("SATEENKAARI_KEYSTORE").orNull
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = providers.gradleProperty("SATEENKAARI_KEYSTORE_PASSWORD").get()
                keyAlias = providers.gradleProperty("SATEENKAARI_KEY_ALIAS").get()
                keyPassword = providers.gradleProperty("SATEENKAARI_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePath != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

ktlint {
    version.set(libs.versions.ktlint)
    android.set(true)
    coloredOutput.set(false)
    ignoreFailures.set(false)
    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.CHECKSTYLE)
    }
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt.yml"))
    source.setFrom("src/main/java", "src/test/java", "src/androidTest/java")
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "17"
    reports {
        xml.required.set(true)
        html.required.set(true)
        sarif.required.set(true)
    }
}

dependencyCheck {
    formats = listOf("HTML", "JSON")
    outputDirectory = rootProject.layout.projectDirectory.dir("reports")
    scanConfigurations = listOf("debugRuntimeClasspath", "releaseRuntimeClasspath")
    suppressionFiles = listOf(rootProject.file("config/dependency-check-suppressions.xml").absolutePath)
    failBuildOnCVSS = 7f
    autoUpdate =
        providers
            .environmentVariable("DEPENDENCY_CHECK_AUTO_UPDATE")
            .map { it.lowercase() in listOf("1", "true", "yes", "on") }
            .getOrElse(true)
    analyzers {
        ossIndex {
            enabled = false
        }
    }
    nvd {
        providers
            .environmentVariable("NVD_API_KEY")
            .orNull
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { apiKey = it }
    }
}

// Version 13.0.0 requires the data directory on tasks to override its built-in default.
tasks.withType<org.owasp.dependencycheck.gradle.tasks.ConfiguredTask>().configureEach {
    data.directory.set(
        providers
            .environmentVariable("DEPENDENCY_CHECK_DATA_DIRECTORY")
            .orElse(
                rootProject.layout.projectDirectory
                    .dir(".gradle/dependency-check-data")
                    .asFile.absolutePath,
            ),
    )
}

// Compose-kääntäjä ja vakausanalysaattori lukevat saman vakauskonfiguraation (domain ja kokoelmat).
val composeStabilityConfig = isolated.rootProject.projectDirectory.file("config/compose-stability.conf")

composeCompiler {
    stabilityConfigurationFiles.add(composeStabilityConfig)
}

composeStabilityAnalyzer {
    stabilityValidation {
        stabilityConfigurationFiles.add(composeStabilityConfig)
    }
}

dependencies {
    lintChecks(libs.android.security.lints)
    detektPlugins(libs.compose.rules.detekt)
    ktlintRuleset(libs.compose.rules.ktlint)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
