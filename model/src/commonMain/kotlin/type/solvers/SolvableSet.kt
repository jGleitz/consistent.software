package software.consistent.model.type.solvers

import software.consistent.model.value.Value

internal sealed interface SolvableSet : ClosedSolvableSet<Value, SolvableSet>
