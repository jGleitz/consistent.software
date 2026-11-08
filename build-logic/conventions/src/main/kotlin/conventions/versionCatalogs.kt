package software.consistent.build.conventions

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.the

val Project.libs
  get() = this.the<VersionCatalogsExtension>().named("libs")
val Project.testLibs
  get() = this.the<VersionCatalogsExtension>().named("testLibs")
val Project.buildLibs
  get() = this.the<VersionCatalogsExtension>().named("buildLibs")

operator fun VersionCatalog.get(identifier: String): Provider<MinimalExternalModuleDependency> =
    this.findLibrary(identifier).orElseThrow {
      InvalidUserDataException("The version catalog ${this.name} has no library '${identifier}'")
    }
