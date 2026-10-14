// AGP 9:n sisäänrakennettu Kotlin tuo oletuksena KGP 2.2.10:n. Uudempi KGP nostetaan classpathiin,
// ja Compose-kääntäjäliitännäisen versio on sama kuin KGP:n.
buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.owasp.dependency.check) apply false
    alias(libs.plugins.stability.analyzer) apply false
    alias(libs.plugins.sonarqube)
}

val sonarProjectProperties =
    java.util.Properties().apply {
        rootProject.file("sonar-project.properties").inputStream().use(::load)
    }

sonar {
    properties {
        sonarProjectProperties.forEach { key, value ->
            property(key.toString(), value.toString())
        }
    }
}

project(":app") {
    sonar {
        properties {
            property(
                "sonar.coverage.jacoco.xmlReportPaths",
                listOf(
                    "reports/coverage/test/debug/report.xml",
                    "reports/coverage/androidTest/debug/connected/report.xml",
                ).joinToString(",") { layout.buildDirectory.file(it).get().asFile.absolutePath },
            )
        }
    }
}

tasks.named("sonar") {
    dependsOn(":app:assembleDebug", ":app:createDebugUnitTestCoverageReport", ":app:createDebugAndroidTestCoverageReport")
}
