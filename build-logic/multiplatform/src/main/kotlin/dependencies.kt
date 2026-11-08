import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency

fun Project.npm(catalogEntry: Provider<MinimalExternalModuleDependency>) =
    catalogEntry.get().let {
      NpmDependency(
          objectFactory = objects,
          name = it.name,
          version =
              requireNotNull(it.version) { "no version specified for npm dependency ${it.name}!" },
      )
    }
