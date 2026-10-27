import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma
import org.jetbrains.kotlin.gradle.tasks.KotlinTest

plugins {
  id("com.google.devtools.ksp")
  id("io.kotest")
}

kotlin {
  sourceSets {
    commonTest {
      dependencies {
        implementation(testLibs["kotest-framework"])
        implementation(testLibs["atrium-fluent"])
      }
    }

    // use named { it == "name"}.configureEach to configure the dependency only when the target is present, without requiring it.
    named { it == "jvmTest" }.configureEach {
      dependencies {
        runtimeOnly(testLibs["kotest-runner-junit5"])
      }
    }
  }
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

tasks.withType<AbstractTestTask>().configureEach {
  // Also invalidate entries written before individualTaskReports stopped KGP from ignoring test failures.
  inputs.property("ignoreFailures", provider { ignoreFailures })
  // With ignoreFailures, a task with failing tests succeeds. KGP’s allTests notices the failures only through test events, which
  // a result restored from the build cache does not replay, so caching such a result would turn the failure into a pass.
  outputs.doNotCacheIf("Failing tests would be cached as passing") { ignoreFailures }
  // A local UP-TO-DATE result would suppress the failure events, too.
  outputs.upToDateWhen { !ignoreFailures }
}

tasks.withType<KotlinTest>().configureEach {
  inputs.property("ignoreRunFailures", provider { ignoreRunFailures })
  outputs.doNotCacheIf("Test runner failures would be cached as passing") { ignoreRunFailures }
  outputs.upToDateWhen { !ignoreRunFailures }
}

// KGP disables caching for JS tests without giving a reason (see KT-78586). We enable it and declare the inputs that KGP leaves
// out. Node modules are covered: the tasks already track the installed package-lock.json.
tasks.withType<KotlinJsTest>().configureEach {
  val testTask = this

  inputs.property("nodeJsVersion", rootProject.the<NodeJsExtension>().nodeVersion)
  inputs.property("operatingSystem", project.providers.systemProperty("os.name"))
  inputs.property("architecture", providers.systemProperty("os.arch"))
  inputs.files("karma.config.d", "webpack.config.d").withPropertyName("jsConfigDirectories").withPathSensitivity(RELATIVE)

  outputs.cacheIf { true }

  onTestFrameworkSet {
    if (this is ManagedChromeFramework) return@onTestFrameworkSet
    if (this is KotlinKarma) {
      val project = testTask.project
      val providers = project.providers
      val objects = project.objects

      val chromeVersion =
        project.buildLibs
          .findVersion("chrome")
          .get()
          .requiredVersion
      val rootPackageDirectory = project.rootProject.the<NodeJsRootExtension>().rootPackageDirectory
      val browserDependencies =
        setOf(
          project.buildLibs.npmPackage("npm-puppeteer-browsers"),
          // Puppeteer's optional peer makes browser downloads honour HTTP(S)_PROXY.
          project.buildLibs.npmPackage("npm-proxy-agent"),
        )
      val cacheDirectory =
        providers.environmentVariable("PUPPETEER_CACHE_DIR").map(::File).orElse(
          providers.systemProperty("user.home").map { File(it, ".cache/puppeteer") },
        )

      testTask.dependsOn(":kotlinNpmInstall")
      testTask.inputs.property("chromeVersion", chromeVersion)
      testTask.testFramework =
        objects.newInstance(
          ManagedChromeFramework::class.java,
          this,
          chromeVersion,
          cacheDirectory.get(),
          rootPackageDirectory,
          browserDependencies,
        )
    }
  }
}
