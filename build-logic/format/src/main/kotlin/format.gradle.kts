plugins {
    id("com.diffplug.spotless")
}

repositories {
    mavenCentral() // to download dependencies of the checkers
}

spotless {
    if (project.hasKotlin) {
        kotlin {
            target(fileTree("src")
                .include("**/*.kt"))
            ktlint("1.6.0")
        }
    }
    kotlinGradle {
        ktlint("1.8.0")
    }
    format("markdown") {
        target("**/*.md")
        prettier()
    }
}

val format by tasks.registering {
    dependsOn("spotlessApply")
}

val lint by tasks.registering {
    dependsOn("spotlessCheck")
}

tasks.named("check").configure {
    dependsOn(lint)
}

val Project.hasKotlin: Boolean get() = plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")
