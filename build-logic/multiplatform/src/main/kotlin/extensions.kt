import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.kotlin(action: KotlinMultiplatformExtension.() -> Unit) = this.extensions.configure(action)
internal val Project.libs get() = this.the<VersionCatalogsExtension>().named("libs")
internal val Project.testLibs get() = this.the<VersionCatalogsExtension>().named("testLibs")

internal operator fun VersionCatalog.get(identifier: String) = this.findLibrary(identifier)
  .orElseThrow { InvalidUserDataException("The version catalog ${this.name} has no library '${identifier}'") }
