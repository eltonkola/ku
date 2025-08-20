@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "io.github.eltonkola"
version = "0.0.3"

kotlin {
    jvm("desktop") {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    wasmJs {
        browser()
        binaries.executable()
    }
//    @OptIn(ExperimentalWasmDsl::class)
//    wasmJs {
//        browser {
//            commonWebpackConfig {
//                cssSupport {
//                    enabled = true
//                }
//            }
//        }
//        binaries.executable()
//    }

    sourceSets {
        val commonMain by getting {
            dependencies {

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.play.services.location)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        val wasmJsMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val androidUnitTest by getting
        val wasmJsTest by getting
    }
}

android {
    namespace = "io.github.eltonkola"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "ku", version.toString())

    pom {
        name = "Ku"
        description = "Simple location handling for Compose Multiplatform."
        inceptionYear = "2024"
        url = "https://github.com/eltonkola/ku/"
        licenses {
            license {
                name.set("Apache 2.0 License")
                url.set("http://www.apache.org/licenses/LICENSE-2.0")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id = "eltonkola"
                name = "Elton Kola"
                url = "eltonkola.com"
            }
        }
        scm {
            url.set("https://github.com/eltonkola/ku")
            connection.set("scm:git:https://github.com/eltonkola/ku.git")
            developerConnection.set("scm:git:ssh://git@github.com:eltonkola/ku.git")
        }
    }
}
