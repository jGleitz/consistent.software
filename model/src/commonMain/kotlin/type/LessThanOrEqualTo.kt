package software.consistent.model.type

import software.consistent.model.type.solvers.DisjunctIntervalUnion
import software.consistent.model.value.Value

public sealed interface LessThanOrEqualTo : Type {
  public val comparisonValue: Value.Number

  public companion object {
    public operator fun invoke(comparisonValue: Value.Number): LessThanOrEqualTo = LessThanOrEqualToImpl(comparisonValue)
  }
}

private class LessThanOrEqualToImpl(override val comparisonValue: Value.Number) : LessThanOrEqualTo,
  SolvableType by UsingSolver(DisjunctIntervalUnion(1) {
    it.appendBoundary(comparisonValue, isInclusive = true)
    it.leftOpen = true
  })
