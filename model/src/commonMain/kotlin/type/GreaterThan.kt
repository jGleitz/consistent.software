package software.consistent.model.type

import software.consistent.model.type.solvers.DisjunctIntervalUnion
import software.consistent.model.value.Value

public sealed interface GreaterThan : Type {
  public val comparisonValue: Value.Number

  public companion object {
    public operator fun invoke(comparisonValue: Value.Number): GreaterThan = GreaterThanImpl(comparisonValue)
  }
}

private class GreaterThanImpl(override val comparisonValue: Value.Number) : GreaterThan,
  SolvableType by UsingSolver(DisjunctIntervalUnion(1) {
    it.appendBoundary(comparisonValue, isInclusive = false)
    it.rightOpen = true
  })
