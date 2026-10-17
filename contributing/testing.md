# Testing

This document sets out how to write tests in this repository.

## Test Edge Cases Explicitly

Tests cover all relevant edge cases, each with an explicit test.
Where appropriate, use Kotest’s [data-driven testing](https://kotest.io/docs/framework/datatesting/data-driven-testing.html) (`withData` and friends) for them.

Don’t rely on the edge cases of a Kotest `Arb` in property tests to test edge cases:

- Readers can’t see at a glance which edge cases apply.
- Kotest doesn’t guarantee that every edge case is tested.
  In Kotest 6.2.5, each argument picks an edge case with a default probability of only 2%, and edge cases don’t run first (see [`config.kt`](https://github.com/kotest/kotest/blob/v6.2.5/kotest-property/src/commonMain/kotlin/io/kotest/property/config.kt)).
- Changing an `Arb` becomes nearly impossible, because the change can have a huge impact on existing tests.
