package software.consistent.data.type.solvers.maps

import software.consistent.data.type.solvers.Cardinality
import software.consistent.data.type.solvers.Cardinality.Companion.exactly
import software.consistent.data.type.solvers.NormalizationComparable
import software.consistent.data.type.solvers.SolvableSet
import software.consistent.data.type.solvers.TypePartitionUnion
import software.consistent.data.util.compare
import software.consistent.formatting.monospacelayout.Monospace
import software.consistent.formatting.monospacelayout.WithMonospaceLayout

internal data class MaybeAbsent(
    val values: SolvableSet,
    val includesAbsent: Boolean,
) : NormalizationComparable<MaybeAbsent>, WithMonospaceLayout {
  fun isEmpty() = !includesAbsent && values.isEmpty()

  fun isTriviallyEmpty() = !includesAbsent && values.isTriviallyEmpty()

  fun isTriviallyNotEmpty() = includesAbsent || values.isTriviallyNotEmpty()

  fun isComplete() = includesAbsent && values.isComplete()

  fun isTriviallyComplete() = includesAbsent && values.isTriviallyComplete()

  fun isFinite() = values.isFinite()

  fun isOnlyAbsent() = includesAbsent && values.isEmpty()

  infix fun with(otherSet: MaybeAbsent) =
      MaybeAbsent(this.values with otherSet.values, this.includesAbsent || otherSet.includesAbsent)

  infix fun overlapWith(otherSet: MaybeAbsent) =
      MaybeAbsent(
          this.values overlapWith otherSet.values,
          this.includesAbsent && otherSet.includesAbsent,
      )

  operator fun not() = MaybeAbsent(!values, !includesAbsent)

  infix fun without(otherSet: MaybeAbsent) =
      MaybeAbsent(
          this.values without otherSet.values,
          this.includesAbsent && !otherSet.includesAbsent,
      )

  infix fun isSubsetOf(largerSet: MaybeAbsent) =
      (!this.includesAbsent || largerSet.includesAbsent) &&
          (this.values isSubsetOf largerSet.values)

  infix fun triviallyEquals(other: MaybeAbsent) =
      this === other ||
          (this.includesAbsent == other.includesAbsent && this.values triviallyEquals other.values)

  fun cardinality(upTo: Int): Cardinality =
      if (includesAbsent) values.cardinality(upTo - 1) + exactly(1) else values.cardinality(upTo)

  override fun normalizationComparedTo(other: MaybeAbsent) =
      (this.values normalizationComparedTo other.values).ifEqual {
        compare(this.includesAbsent).to(other.includesAbsent).case
      }

  override val monospaceLayout
    get() =
        if (includesAbsent) {
          Monospace.line("∪", 2) {
            -values.monospaceLayout.containerWrappedIn("(", ")")
            -Monospace.atom("Ω")
          }
        } else values.monospaceLayout

  override fun toString(): String = monospaceLayout.renderToString()

  companion object {
    private val empty = MaybeAbsent(TypePartitionUnion.empty(), false)
    private val complete = MaybeAbsent(TypePartitionUnion.complete(), true)
    private val onlyAbsent = MaybeAbsent(TypePartitionUnion.empty(), true)

    internal fun empty() = empty

    internal fun complete() = complete

    internal fun onlyAbsent() = onlyAbsent
  }
}
