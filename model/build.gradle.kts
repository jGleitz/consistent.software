@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.gradle.internal.declarativedsl.ignoreSomeNonDeclarativeSyntaxWeCurrentlyHaveNoSolutionFor
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform") version "2.2.21"
    id("com.google.devtools.ksp") version "2.3.2"
    id("io.kotest") version "6.0.0"
}

kotlin {
    jvm()
    js {
        browser()
        nodejs()
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
                implementation("io.kotest:kotest-framework-engine:6.0.0")
                implementation("ch.tutteli.atrium:atrium-fluent:1.2.0")
            }
        }
    }
}

repositories {
    mavenCentral()
}