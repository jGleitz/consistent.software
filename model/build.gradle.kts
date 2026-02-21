@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform") version "2.2.21"
    id("com.google.devtools.ksp") version "2.3.2"
    id("io.kotest") version "6.1.3"
}

kotlin {
    jvm()
    js {
        browser()
        nodejs()
        useEsModules()
        generateTypeScriptDefinitions()
    }
    linuxX64()

    applyDefaultHierarchyTemplate {
        common {
            group("nonNative") {
                withJvm()
                withJs()
            }
        }
    }

    sourceSets {
        named("nonNativeTest") {
            dependencies {
                implementation("io.kotest:kotest-framework-engine:6.1.3")
                implementation("ch.tutteli.atrium:atrium-fluent:1.2.0")
            }
        }
        jvmTest {
            dependencies {
                implementation("io.kotest:kotest-runner-junit5:6.1.3")
            }
        }
    }
}

repositories {
    mavenCentral()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}