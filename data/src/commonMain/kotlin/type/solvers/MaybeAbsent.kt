package software.consistent.data.type.solvers

import software.consistent.data.util.compare

internal data class MaybeAbsent(
  val values: SolvableSet,
  val includesAbsent: Boolean
) : SetOperations<MaybeAbsent>, NormalizationComparable<MaybeAbsent> {
  override fun isEmpty() = !includesAbsent && values.isEmpty()

  override fun with(otherSet: MaybeAbsent) =
    MaybeAbsent(this.values with otherSet.values, this.includesAbsent || otherSet.includesAbsent)

  override fun overlapWith(otherSet: MaybeAbsent) =
    MaybeAbsent(this.values overlapWith otherSet.values, this.includesAbsent && otherSet.includesAbsent)

  override fun not() = MaybeAbsent(!values, !includesAbsent)

  override fun without(otherSet: MaybeAbsent) =
    MaybeAbsent(this.values without otherSet.values, this.includesAbsent && !otherSet.includesAbsent)

  override fun isSubsetOf(largerSet: MaybeAbsent) =
    (!this.includesAbsent || largerSet.includesAbsent) && (this.values isSubsetOf largerSet.values)

  override fun normalizationComparedTo(other: MaybeAbsent) = (this.values normalizationComparedTo other.values)
    .ifEqual { compare(this.includesAbsent).to(other.includesAbsent).case }

  companion object {
    private val empty = MaybeAbsent(PartitionedTypeUnion.empty(), false)
    private val complete = MaybeAbsent(PartitionedTypeUnion.complete(), true)
    private val onlyAbsent = MaybeAbsent(PartitionedTypeUnion.empty(), true)

    internal fun empty() = empty
    internal fun complete() = complete
    internal fun onlyAbsent() = onlyAbsent
  }
}
