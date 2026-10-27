import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

val Project.javaReleaseVersion get() = providers.gradleProperty("javaReleaseVersion").map(JavaLanguageVersion::of)
val Project.kotlinReleaseVersion get() = providers.gradleProperty("kotlinReleaseVersion").map(KotlinVersion::fromVersion)
