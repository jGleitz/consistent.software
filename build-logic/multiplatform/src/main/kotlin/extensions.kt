import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.NpmPackageVersion

internal fun Project.kotlin(action: KotlinMultiplatformExtension.() -> Unit) = this.extensions.configure(action)
internal val Project.libs get() = this.the<VersionCatalogsExtension>().named("libs")
internal val Project.testLibs get() = this.the<VersionCatalogsExtension>().named("testLibs")
internal val Project.buildLibs get() = this.the<VersionCatalogsExtension>().named("buildLibs")

internal fun VersionCatalog.npmPackage(identifier: String): NpmPackageVersion {
  val dependency = this[identifier].get()
  require(dependency.module.group == "npm") { "Expected an npm version-catalog dependency, got ${dependency.module}" }
  return NpmPackageVersion(dependency.module.name, dependency.versionConstraint.requiredVersion)
}

internal operator fun VersionCatalog.get(identifier: String) = this.findLibrary(identifier)
  .orElseThrow { InvalidUserDataException("The version catalog ${this.name} has no library '${identifier}'") }
