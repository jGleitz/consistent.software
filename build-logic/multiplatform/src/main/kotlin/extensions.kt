import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

internal fun Project.kotlin(action: KotlinMultiplatformExtension.() -> Unit) = this.extensions.configure(action)
internal val Project.libs get() = this.the<VersionCatalogsExtension>().named("libs")
internal val Project.testLibs get() = this.the<VersionCatalogsExtension>().named("testLibs")
internal val Project.buildLibs get() = this.the<VersionCatalogsExtension>().named("buildLibs")

internal fun KotlinDependencyHandler.devNpm(dependency: Provider<MinimalExternalModuleDependency>) = dependency.map {
  require(it.module.group == "npm") { "Expected an npm version-catalog dependency, got ${it.module}" }
  devNpm(it.module.name, it.versionConstraint.requiredVersion)
}

internal operator fun VersionCatalog.get(identifier: String) = this.findLibrary(identifier)
  .orElseThrow { InvalidUserDataException("The version catalog ${this.name} has no library '${identifier}'") }
