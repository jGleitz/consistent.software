# Build Logic

This directory and its subdirectories form an [included build](https://docs.gradle.org/current/userguide/composite_builds.html) containing the project’s build logic. The main project and its subprojects configure themselves primarily by applying the [convention plugins](https://docs.gradle.org/current/userguide/implementing_gradle_plugins_convention.html) defined in this directory. See Gradle’s best practice ‘[Favor `build-logic` Composite Builds for Build Logic](https://docs.gradle.org/current/userguide/best_practices_structuring_builds.html#favor_composite_builds)’ for more information on this pattern.

## Node.js and formatting

The `nodejs` convention plugin installs Node.js once in the root project and exposes stable executable paths to all projects. In the `format` convention plugin, register Prettier formats with `prettierFormat(name) { ... }`. This helper registers the format and then adds the root `:linkNpm` dependency to its task provider, outside Spotless's deferred formatter configuration. Kotlin-only formats do not depend on Node.js. Do not infer setup dependencies from formatter step names: `toggleOffOn` wraps the steps and hides the original Prettier step.

If Spotless has already recorded a failed npm launch as a lint result, run it with `--rerun-tasks` after fixing the setup dependency to recompute that result.
