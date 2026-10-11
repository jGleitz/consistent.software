package software.consistent.data.type.solvers

import software.consistent.data.value.Value

internal sealed interface SolvableSet : ClosedSolvableSet<Value, SolvableSet>
