package software.consistent.data.type.solvers

import software.consistent.formatting.monospacelayout.WithMonospaceLayout

internal interface ClosedSolvableSet<TValue, TPartition : ClosedSolvableSet<TValue, TPartition>> :
    NormalizationComparable<TPartition>, WithMonospaceLayout {
  operator fun contains(value: TValue): Boolean

  /**
   * Provides one member of this set.
   *
   * Implementations aim to pick a value that is ‘simple’ when that is possible without significant
   * impact on performance.
   *
   * @return `null` if, and only if, this set [isEmpty]. Else, a value that is contained in this
   *   set.
   */
  fun anyMember(): TValue?

  // TODO evaluate whether an eager collection or a lazy sequence is more appropriate here
  val members: Sequence<TValue>

  fun isEmpty(): Boolean

  /**
   * Whether this set can trivially be determined to be empty. If this method returns `true`, then
   * [isEmpty] definitely returns `true`. If it returns `false`, the set might still be empty.
   *
   * Implementations must run in O(1).
   */
  fun isTriviallyEmpty(): Boolean

  /**
   * Whether this set can trivially be determined to contain at least one value. If this method
   * returns `true`, then [isEmpty] definitely returns `false`. If it returns `false`, the set might
   * still contain values.
   *
   * Implementations must run in O(1).
   */
  fun isTriviallyNotEmpty(): Boolean

  fun isComplete(): Boolean

  /**
   * Whether this set can trivially be determined to be complete. If this method returns `true`,
   * then [isComplete] definitely returns `true`. If it returns `false`, the set might still be
   * complete.
   *
   * Implementations must run in O(1).
   */
  fun isTriviallyComplete(): Boolean

  /**
   * Whether this set can trivially be determined to contain exactly the same values as [otherSet].
   * If this method returns `true`, then the sets definitely contain the same values. If it returns
   * `false`, the sets might still contain the same values.
   *
   * Implementations must run in O(n), where `n` is a reasonable measure of `this`’s syntactic size.
   */
  infix fun triviallyEquals(otherSet: ClosedSolvableSet<*, *>): Boolean

  fun isFinite(): Boolean

  /**
   * Determines the set cardinality’s exact value up until the boundary [upTo], and a lower bound
   * thereafter. This method cannot be used to determine finiteness, use [isFinite] for that
   * purpose.
   *
   * @param upTo The maximum cardinality that is of interest to the caller. Implementations may use
   *   this to cut the — potentially expensive — cardinality computation once they know the
   *   cardinality is higher than this value. Must be at least `0`.
   * @return <!-- -->
   *     - If this set has [upTo] members or fewer: [Cardinality.exactly] with the exact value.
   *     - Else, if this set has more than [upTo], but at most [Int.MAX_VALUE] members: either
   *       [Cardinality.exactly] with the exact value, or [Cardinality.atLeast] with a lower bound
   *       greater than [upTo] but not exceeding the actual cardinality.
   *     - Else, if the set has more than [Int.MAX_VALUE] or infinitely many members: either
   *       [Cardinality.moreThanIntMaxValue] or [Cardinality.atLeast] with a lower bound greater
   *       than [upTo].
   */
  fun cardinality(upTo: Int): Cardinality

  /** The set of all values that are not in this set. */
  operator fun not(): TPartition

  /** The set of all values that are in this set **or** in the [otherSet] (‘union’). */
  infix fun with(otherSet: TPartition): TPartition

  /** The set of all values that are in this set **and** in the [otherSet] (‘intersection’). */
  infix fun overlapWith(otherSet: TPartition): TPartition

  /** The set of all values that are in this set **but not** in the [otherSet] (‘difference’). */
  infix fun without(otherSet: TPartition): TPartition

  /**
   * Whether the [largerSet] contains every value in this set. This includes the case that
   * [largerSet] contains the same values as this set.
   */
  infix fun isSubsetOf(largerSet: TPartition): Boolean

  /** Whether there is at least one value in this set that is not in the [largerSet]. */
  infix fun isNotSubsetOf(largerSet: TPartition): Boolean = !isSubsetOf(largerSet)
}

internal fun ClosedSolvableSet<*, *>.mustNotRequestMembersOfInfiniteType() =
    IllegalStateException("members called on infinite $this") // TODO better exception

internal fun ClosedSolvableSet<*, *>.doesNotSupportHashing() =
    UnsupportedOperationException("${ClosedSolvableSet::class.simpleName}s do not support hashing!")
