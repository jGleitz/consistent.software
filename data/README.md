## Test strategy

To the largest extent possible, we write tests only against the _public_ API of this module. This allows us to have an extensive test suite without incurring high refactoring costs for changing or optimizing the solver layer.

As a consequence, we cannot use class- or even method-based unit tests to trace an error back to its source. Instead, we invest into the debuggability of our code base, using well-crafted `toString()` representations and code structures that can be inspected well.

### Property Testing

Covering the enormous combinatory space of relevant different inputs just by classical unit tests would lead to an unmaintainable large number of test cases. Therefore, we supplement our classical unit tests with property-based tests that assert certain behaviour across a wide range of generated inputs.

1.  This requires care when writing the input generators: They must cover all relevant cases.
2.  We don’t want to rely exclusively on property tests, which are often a little harder to debug. Hence, we cover the ‘main’ cases as well as specific edge cases with classical unit tests. Where the line falls requires judgement and often won’t have a single true answer.
