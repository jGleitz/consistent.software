package software.consistent.model.type.solvers

internal sealed interface ClosedSolvableSet<TValue, TPartition : ClosedSolvableSet<TValue, TPartition>> :
  ValueSet.NormalizationComparable<TValue, TPartition>, SetOperations<TPartition> {
  fun anyMember(): TValue?
  // TODO evaluate whether an eager collection or a lazy sequence is more appropriate here
  val members: Sequence<TValue>
  fun isComplete(): Boolean
  fun isFinite(): Boolean
  fun cardinality(max: Int): Int

}

internal fun ClosedSolvableSet<*, *>.mustNotRequestCardinalityOfInfiniteType() =
  IllegalStateException("cardinality called on infinite $this") // TODO better exception

internal fun ClosedSolvableSet<*, *>.mustNotRequestMembersOfInfiniteType() =
  IllegalStateException("members called on infinite $this") // TODO better exception
