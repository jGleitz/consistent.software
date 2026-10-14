# Data: Architecture

## Test Strategy

As far as possible, we test only against the _public_ API of this module.
This lets us keep an extensive test suite without incurring high refactoring costs when we change or optimise the solver layer.

As a consequence, we cannot rely on class- or method-level unit tests to trace an error back to its source.
Instead, we invest in the debuggability of our code base: well-crafted `toString()` representations and code structures that are easy to inspect.

### Property Testing

Covering the enormous combinatorial space of relevant inputs with classical unit tests alone would require an unmaintainably large number of test cases.
We therefore supplement our classical unit tests with property-based tests that assert behaviour across a wide range of generated inputs.

1.  Input generators need care: they must cover all relevant cases.
2.  We don’t rely on property tests alone, as they are often harder to debug.
    We also cover the main cases and specific edge cases with classical unit tests.
    Where to draw the line is a matter of judgement and often has no single right answer.
