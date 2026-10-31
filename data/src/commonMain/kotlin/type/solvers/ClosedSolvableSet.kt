package software.consistent.data.type.solvers

import software.consistent.data.formatting.monospacelayout.WithMonospaceLayout
import kotlin.jvm.JvmInline
import kotlin.math.abs

internal sealed interface ClosedSolvableSet<TValue, TPartition : ClosedSolvableSet<TValue, TPartition>> :
  ValueSet.NormalizationComparable<TValue, TPartition>,
  SetOperations<TPartition>,
  WithMonospaceLayout {
  fun isEmpty(): Boolean

  /**
   * Whether the fact that this set is empty can be determined trivially by looking at its syntactic structure. If this method returns
   * `true`, then the set is definitely empty. If it returns `false`, [isEmpty] might still return `true`.
   *
   * Implementations must run in O(1).
   */
  fun isTriviallyEmpty(): Boolean

  /**
   * Provides one member of this set.
   *
   * Implementations aim to pick a value that is ‘simple’ when that is possible without significant impact on performance.
   *
   * @return `null` if, and only if, this set [isEmpty]. Else, a value that is contained in this set.
   */
  fun anyMember(): TValue?

  // TODO evaluate whether an eager collection or a lazy sequence is more appropriate here
  val members: Sequence<TValue>

  /**
   * Whether the fact that this set is complete can be determined trivially by looking at its syntactic structure. If this method returns
   * `true`, then the set is definitely complete. If it returns `false`, [isComplete] might still return `true`.
   *
   * Implementations must run in O(1).
   */
  fun isTriviallyComplete(): Boolean

  fun isComplete(): Boolean

  fun isFinite(): Boolean

  /**
   * Determines the set’s cardinality exactly up until the boundary [upTo], and with a lower bound thereafter.
   * This method cannot be used to determine finiteness, use [isFinite] for that purpose.
   *
   * @param upTo The maximum cardinality that is of interest to the caller. Implementations may use this to cut the — potentially expensive
   * — cardinality computation once they know the cardinality is higher than this value. Must be at least `0`.
   * @return <!-- -->
   *   - If this set has [upTo] members or fewer: [Cardinality.exactly] with the exact value.
   *   - Else, if this set has more than [upTo], but at most [Int.MAX_VALUE] members: either [Cardinality.exactly] with the exact value,
   *     or [Cardinality.atLeast] with a lower bound greater than [upTo] but not exceeding the actual cardinality.
   *   - Else, if the set has more than [Int.MAX_VALUE] or infinitely many members: either [Cardinality.moreThanIntMaxValue] or
   *     [Cardinality.atLeast] with a lower bound greater than [upTo].
   */
  fun cardinality(upTo: Int): Cardinality

  /**
   * The set of all values that are not in this set.
   */
  operator fun not(): TPartition

  /**
   * The set of all values that are in this set **or** in the [otherSet] (‘union’).
   */
  infix fun with(otherSet: TPartition): TPartition

  /**
   * The set of all values that are in this set **and** in the [otherSet] (‘intersection’).
   */
  infix fun overlapWith(otherSet: TPartition): TPartition

  /**
   * The set of all values that are in this set **but not** in the [otherSet] (‘difference’).
   */
  infix fun without(otherSet: TPartition): TPartition

  /**
   * Whether the [largerSet] contains every value in this set. This includes the case that [largerSet] contains the same values as this set.
   */
  infix fun isSubsetOf(largerSet: TPartition): Boolean

  /**
   * Whether there is at least one value in this set that is not in the [largerSet].
   */
  infix fun isNotSubsetOf(largerSet: TPartition): Boolean = !isSubsetOf(largerSet)

  @JvmInline
  value class Cardinality(
    // Int.MIN_VALUE: more than Int.MAX_VALUE
    // negative: at least (-value), might be higher
    // positive: exactly value
    private val value: Int,
  ) {
    companion object {
      fun moreThanIntMaxValue() = Cardinality(Int.MIN_VALUE)

      fun atLeast(value: Int): Cardinality {
        require(value > 0) {
          // TODO better exception?
          "Argument to ${::atLeast.name} must be positive (got $value)"
        }
        return Cardinality(-value)
      }

      fun exactly(value: Int): Cardinality {
        require(value >= 0) {
          // TODO better exception?
          "Argument to ${::exactly.name} must not be negative (got $value)"
        }
        return Cardinality(value)
      }
    }

    val lowerBound: Int
      get() =
        when {
          value == Int.MIN_VALUE -> Int.MAX_VALUE
          value < 0 -> -value
          else -> value
        }

    val exactValue: Int
      get() {
        require(value >= 0) { "Not an exact cardinaality: $this" }
        return value
      }

    operator fun plus(other: Cardinality) =
      when {
        this.value == Int.MIN_VALUE || other.value == Int.MIN_VALUE -> {
          moreThanIntMaxValue()
        }

        this.value < 0 || other.value < 0 -> {
          val sum = abs(this.value) + abs(other.value)
          if (sum >= 0) atLeast(sum) else moreThanIntMaxValue() // overflow
        }

        else -> {
          val sum = this.value + other.value
          if (sum >= 0) exactly(sum) else moreThanIntMaxValue() // overflow
        }
      }

    operator fun compareTo(otherValue: Int): Int {
      require(otherValue >= 0) {
        // TODO better exception?
        "Argument to ${::compareTo.name} must not be negative (got $otherValue)"
      }
      return when {
        this.value == Int.MIN_VALUE -> {
          1
        }

        this.value >= 0 -> {
          this.value - otherValue
        }

        else -> {
          val absValue = -value
          require(otherValue < absValue) {
            // TODO better exception?
            "This cardinality (at least $absValue) can only be compared to values up to ${absValue - 1} (got $otherValue)"
          }
          absValue - otherValue
        }
      }
    }
  }
}

internal fun ClosedSolvableSet<*, *>.mustNotRequestMembersOfInfiniteType() =
  IllegalStateException("members called on infinite $this") // TODO better exception
