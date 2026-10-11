package software.consistent.data.type.solvers

import software.consistent.data.formatting.monospacelayout.WithMonospaceLayout

internal sealed interface ClosedSolvableSet<TValue, TPartition : ClosedSolvableSet<TValue, TPartition>> :
  ValueSet.NormalizationComparable<TValue, TPartition>, SetOperations<TPartition>, WithMonospaceLayout {
  /**
   * Whether the fact that this set is empty can be determined trivially by looking at its syntactic structure. If this method returns
   * `true`, then the set is definitely empty. If it returns `false`, [isEmpty] might still return `true`.
   */
  fun isTriviallyEmpty(): Boolean
  fun anyMember(): TValue?

  // TODO evaluate whether an eager collection or a lazy sequence is more appropriate here
  val members: Sequence<TValue>

  /**
   * Whether the fact that this set is complete can be determined trivially by looking at its syntactic structure. If this method returns
   * `true`, then the set is definitely complete. If it returns `false`, [isComplete] might still return `true`.
   */
  fun isComplete(): Boolean
  fun isTriviallyComplete(): Boolean
  fun isFinite(): Boolean
  fun cardinality(max: Int): Int

}

internal fun ClosedSolvableSet<*, *>.mustNotRequestCardinalityOfInfiniteType() =
  IllegalStateException("cardinality called on infinite $this") // TODO better exception

internal fun ClosedSolvableSet<*, *>.mustNotRequestMembersOfInfiniteType() =
  IllegalStateException("members called on infinite $this") // TODO better exception
