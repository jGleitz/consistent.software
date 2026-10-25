# Testing

This document sets out how we test the code in this repository.

## Running Tests

Run `mise exec -- ./gradlew allTests --continue` to test all targets, or use a target task such as `jvmTest` or `jsNodeTest`.
The build enables Kotlin’s `kotlin.tests.individualTaskReports` option so that failing tests fail their own task and cannot be cached as successful runs.
Without `--continue`, Gradle stops scheduling new tasks after a failure.
Individual HTML and JUnit XML reports remain available under each module’s `build/reports/tests/` and `build/test-results/`; the aggregate `allTests` HTML report may not be generated when a dependency fails.

JavaScript test results are cached, including the version of the Node executable selected by mise.
Browser test caching tracks the version of the executable selected by Karma’s ChromeHeadless launcher, including its default browser lookup when `CHROME_BIN` is unset.
Unchanged tests restored from the cache do not explore new random inputs in property tests.
Use `--rerun-tasks` when you want fresh test execution.

## Test Against the Public API

As far as possible, we test each module only against its _public_ API.
This lets us keep an extensive test suite without incurring high refactoring costs when we change or optimise a module’s internals.

As a consequence, we can’t rely on class- or method-level unit tests to trace an error back to its source.
Instead, we invest in the debuggability of our code base: well-crafted `toString()` representations and code structures that are easy to inspect.

## Test Edge Cases Explicitly

Tests cover all relevant edge cases, each with an explicit test.
Where appropriate, use Kotest’s [data-driven testing](https://kotest.io/docs/framework/datatesting/data-driven-testing.html) (`withData` and friends) for them.

## Property Testing

Where the space of relevant inputs is too large to cover with classical unit tests alone, we supplement them with property-based tests that assert behaviour across a wide range of generated inputs.

1.  Input generators need care: they must cover all relevant cases.
2.  We don’t rely on property tests alone, as they are often harder to debug.
    We also cover the main cases with classical unit tests, and edge cases with explicit tests as described above.
    Where to draw the line is a matter of judgement and often has no single right answer.

Don’t rely on the edge cases of a Kotest `Arb` to test edge cases:

- Readers can’t see at a glance which edge cases apply.
- Kotest doesn’t guarantee that every edge case is tested.
  In Kotest 6.2.5, each argument picks an edge case with a default probability of only 2%, and edge cases don’t run first (see [`config.kt`](https://github.com/kotest/kotest/blob/v6.2.5/kotest-property/src/commonMain/kotlin/io/kotest/property/config.kt)).
- Changing an `Arb` becomes nearly impossible, because the change can have a huge impact on existing tests.
