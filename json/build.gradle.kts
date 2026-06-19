@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  id("format")
  id("multiplatform")
}

dependencies {
  commonMainImplementation(project(":parsers:json"))
}
