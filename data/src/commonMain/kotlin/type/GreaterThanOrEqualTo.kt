package software.consistent.data.type

import software.consistent.data.formatting.monospacelayout.WithMonospaceLayout
import software.consistent.data.type.solvers.DisjunctIntervalUnion
import software.consistent.data.value.Value
import software.consistent.data.value.monospaceLayout

public sealed interface GreaterThanOrEqualTo : Type {
  public val comparisonValue: Value.Number

  public companion object {
    public operator fun invoke(comparisonValue: Value.Number): GreaterThanOrEqualTo = GreaterThanOrEqualToImpl(comparisonValue)
  }
}

private class GreaterThanOrEqualToImpl(override val comparisonValue: Value.Number) : GreaterThanOrEqualTo, WithMonospaceLayout,
  Queryable by UsingSolver(DisjunctIntervalUnion(1) {
    it.appendBoundary(comparisonValue, isInclusive = true)
    it.rightOpen = true
  }) {
  override val monospaceLayout get() = comparisonValue.monospaceLayout.withPrefix("≥ ")
  override fun toString() = monospaceLayout.renderToString()
}
