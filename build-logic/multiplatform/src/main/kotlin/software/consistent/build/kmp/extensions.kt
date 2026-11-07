package software.consistent.build.kmp

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency

internal fun Project.kotlin(action: KotlinMultiplatformExtension.() -> Unit) = this.extensions.configure(action)
internal val Project.libs get() = this.the<VersionCatalogsExtension>().named("libs")
internal val Project.testLibs get() = this.the<VersionCatalogsExtension>().named("testLibs")

internal operator fun VersionCatalog.get(identifier: String) = this.findLibrary(identifier)
  .orElseThrow { InvalidUserDataException("The version catalog ${this.name} has no library '${identifier}'") }

fun Project.npm(catalogEntry: Provider<MinimalExternalModuleDependency>) =
  catalogEntry.get().let {
    NpmDependency(
      objectFactory = objects,
      name = it.name,
      version = requireNotNull(it.version) { "no version specified for npm dependency ${it.name}!" },
    )
  }
