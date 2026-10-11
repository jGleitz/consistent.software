package software.consistent.model.type.solvers

import software.consistent.model.invariants.Invariants.hasSameSizeAs
import software.consistent.model.invariants.Invariants.isNonDecreasing
import software.consistent.model.invariants.checkInvariant
import software.consistent.model.util.ComparisonCase.*
import software.consistent.model.util.compare
import software.consistent.model.util.compareEach
import software.consistent.model.value.Value

/**
 *  [orderedBoundaries] holds the interval boundaries, [inclusive] indicates for each boundary whether it’s inclusive.
 *  A pair of two consecutive [orderedBoundaries] represents one interval. If [leftOpen], then the first interval is open on the left.
 *  If [rightOpen], then the last interval is open on the right.
 *
 *  For example:
 *  ```kotlin
 *  DisjointIntervals(
 *    boundaries =         listOf(   3,     8,    20,   42),
 *    inclusive  = booleanArrayOf(true, false, false, true),
 *    leftOpen   = false
 *    rightOpen  = false
 *  )
 *  ```
 *  represents the set `[3,8) ∪ (20,42]`.
 *
 *  ```kotlin
 *  DisjointIntervals(
 *    boundaries =         listOf(   3,    5,    5),
 *    inclusive  = booleanArrayOf(true, true, true),
 *    leftOpen   = true,
 *    rightOpen  = false
 *  )
 *  ```
 *  represents the set `(-∞,3] ∪ {5}`.
 */
internal class DisjunctIntervalUnion<TValue>
private constructor(
  private val orderedBoundaries: List<TValue>,
  private val inclusive: BooleanArray,
  private val leftOpen: Boolean,
  private val rightOpen: Boolean
) : ClosedSolvableSet<TValue, DisjunctIntervalUnion<TValue>> where TValue : Value, TValue : Comparable<TValue> {
  init {
    ::orderedBoundaries hasSameSizeAs ::inclusive
    ::orderedBoundaries.isNonDecreasing
    checkInvariant(
      """consecutive equal boundaries are only allowed if:
           * they belong to the same interval and both boundaries are inclusive, or
           * they belong to consecutive intervals and both boundaries are exclusive
           """.trimIndent(),
      ::orderedBoundaries, ::inclusive, ::leftOpen
    ) { boundaries, inclusive, leftOpen ->
      when (boundaries.size) {
        0, 1 -> true
        else -> {
          var last = boundaries[0]
          for (i in 1 until boundaries.size) {
            if (boundaries[i].compareTo(last) == 0) {
              val belongToSameInterval = (i % 2 == 0) == leftOpen
              if (belongToSameInterval && !(inclusive[i] && inclusive[i - 1])) {
                return@checkInvariant false
              }
              if (!belongToSameInterval && (inclusive[i] || inclusive[i - 1])) {
                return@checkInvariant false
              }
            }
          }
          true
        }
      }
    }
    checkInvariant(
      "boundaries.size + leftOpen + rightOpen must be even",
      ::orderedBoundaries, ::leftOpen, ::rightOpen
    ) { boundaries, leftOpen, rightOpen ->
      (boundaries.size % 2) + (if (leftOpen != rightOpen) 1 else 0) == 0
    }
  }

  override fun contains(value: TValue) = isInsideInterval(findPositionOf(value))

  private fun isClosed() = !leftOpen && !rightOpen

  override fun isEmpty() = orderedBoundaries.isEmpty() && isClosed()

  override fun isComplete() = orderedBoundaries.isEmpty() && !isClosed()

  override fun isFinite(): Boolean {
    if (!isClosed()) return false

    var i = 0
    while (i <= orderedBoundaries.lastIndex) {
      if (orderedBoundaries[i] != orderedBoundaries[i + 1]) return false
      i += 2
    }
    return true
  }

  override fun cardinality(max: Int) = if (isEmpty()) 0 else throw mustNotRequestCardinalityOfInfiniteType()

  override val members
    get() = when {
      !isClosed() -> throw mustNotRequestMembersOfInfiniteType()
      orderedBoundaries.isEmpty() -> emptySequence<TValue>()
      else -> (0 until (orderedBoundaries.size / 2)).asSequence().map { intervalIndex ->
        val lowerBoundary = orderedBoundaries[intervalIndex * 2]
        if (lowerBoundary != orderedBoundaries[intervalIndex * 2 + 1]) throw mustNotRequestCardinalityOfInfiniteType()
        lowerBoundary
      }
    }

  override fun anyMember(): TValue? {
    if (orderedBoundaries.isEmpty()) {
      return if (leftOpen && rightOpen) exampleBetween(null, null)
      else null
    }
    val firstInclusiveIndex = inclusive.indexOfFirst { it }
    if (firstInclusiveIndex != -1) return orderedBoundaries[firstInclusiveIndex]
    return if (leftOpen) exampleBetween(null, orderedBoundaries[0])
    else if (orderedBoundaries.size == 1) exampleBetween(orderedBoundaries[0], null)
    else exampleBetween(orderedBoundaries[0], orderedBoundaries[1])
  }

  override fun with(otherSet: DisjunctIntervalUnion<TValue>) =
    DisjunctIntervalUnion(this.orderedBoundaries.size + otherSet.orderedBoundaries.size) { result ->
      var thisIndex = 0
      val thisBoundaryLength = this.orderedBoundaries.size
      var otherIndex = 0
      val otherBoundaryLength = otherSet.orderedBoundaries.size

      while (thisIndex < thisBoundaryLength && otherIndex < otherBoundaryLength) {
        when (compare(this.orderedBoundaries[thisIndex]).to(otherSet.orderedBoundaries[otherIndex]).case) {
          isLower -> {
            if (otherSet.opensInterval(otherIndex)) {
              // The other boundary opens a new interval. So either:
              //  * this boundary opens an earlier interval => we should add it to the result.
              //  * this boundary closes the current interval before the next starts => we should add it to the result.
              result.appendBoundary(this.orderedBoundaries[thisIndex], this.inclusive[thisIndex])
            }
            // else: We are inside an interval opened by otherSet. So either:
            //  * this boundary opens an interval inside the other one => we can skip this boundary
            //  * this boundary closes an interval while the other is still open => we can skip this boundary
            thisIndex += 1
          }

          isEqual -> {
            if (this.opensInterval(thisIndex) == otherSet.opensInterval(otherIndex)) {
              // The boundaries agree on whether they open or close an interval.
              result.appendBoundary(this.orderedBoundaries[thisIndex], this.inclusive[thisIndex] || otherSet.inclusive[otherIndex])
            } else if (!this.inclusive[thisIndex] && !otherSet.inclusive[otherIndex]) {
              // One boundary closes, the other opens, and both are exclusive. There is exactly one item that fits in between them: the
              // boundary itself. We can’t skip the boundary and need to add it.
              result.appendBoundary(this.orderedBoundaries[thisIndex], isInclusive = false)
              result.appendBoundary(this.orderedBoundaries[thisIndex], isInclusive = false)
            }
            // else: One boundary closes, the other one opens, and at least one of them is inclusive. There is no item that could fit in between
            // them. The currently open interval just stays open => we can skip both boundaries.
            thisIndex += 1
            otherIndex += 1
          }

          isHigher -> {
            if (this.opensInterval(thisIndex)) {
              result.appendBoundary(otherSet.orderedBoundaries[otherIndex], otherSet.inclusive[otherIndex])
            }
            otherIndex += 1
          }
        }
      }

      for (i in (thisIndex until thisBoundaryLength)) {
        result.appendBoundary(this.orderedBoundaries[i], this.inclusive[i])
      }
      for (i in otherIndex until otherBoundaryLength) {
        result.appendBoundary(otherSet.orderedBoundaries[i], otherSet.inclusive[i])
      }
      result.leftOpen = this.leftOpen || otherSet.leftOpen
      result.rightOpen = this.rightOpen || otherSet.rightOpen
    }

  override fun overlapWith(otherSet: DisjunctIntervalUnion<TValue>) =
    DisjunctIntervalUnion(this.orderedBoundaries.size + otherSet.orderedBoundaries.size) { result ->
      var thisIndex = 0
      val thisBoundaryLength = this.orderedBoundaries.size
      var otherIndex = 0
      val otherBoundaryLength = otherSet.orderedBoundaries.size

      while (thisIndex < thisBoundaryLength && otherIndex < otherBoundaryLength) {
        when (compare(this.orderedBoundaries[thisIndex]).to(otherSet.orderedBoundaries[otherIndex]).case) {
          isLower -> {
            if (otherSet.closesInterval(otherIndex)) {
              // The other boundary closes its currently open interval. So either:
              //  * this boundary opens its interval => now both are open and the result interval can open
              //  * this boundary closes its interval => no only one is open, and we must close the result interval
              result.appendBoundary(this.orderedBoundaries[thisIndex], this.inclusive[thisIndex])
            }
            // else: We are not inside an interval of otherSet. Hence, we drop our boundary.
            thisIndex += 1
          }

          isEqual -> {
            if (this.opensInterval(thisIndex) == otherSet.opensInterval(otherIndex)) {
              // The boundaries agree on whether they open or close an interval.
              result.appendBoundary(this.orderedBoundaries[thisIndex], this.inclusive[thisIndex] && otherSet.inclusive[otherIndex])
            } else if (this.inclusive[thisIndex] && otherSet.inclusive[otherIndex]) {
              // One boundary closes, the other opens, and both are inclusive. There is exactly one item in the overlap: the
              // boundary itself. We can’t skip the boundary and need to add it.
              result.appendBoundary(this.orderedBoundaries[thisIndex], isInclusive = true)
              result.appendBoundary(this.orderedBoundaries[thisIndex], isInclusive = true)
            }
            // Else: One boundary closes, the other one opens, and at least one of them is exclusive. There is no overlap, we skip both
            // boundaries
            thisIndex += 1
            otherIndex += 1
          }

          isHigher -> {
            if (this.closesInterval(thisIndex)) {
              result.appendBoundary(otherSet.orderedBoundaries[otherIndex], otherSet.inclusive[otherIndex])
            }
            otherIndex += 1
          }
        }
      }
      result.leftOpen = this.leftOpen && otherSet.leftOpen
      result.rightOpen = this.rightOpen && otherSet.rightOpen
    }

  override fun not() =
    DisjunctIntervalUnion(orderedBoundaries, BooleanArray(inclusive.size) { !inclusive[it] }, !leftOpen, !rightOpen)

  override fun without(otherSet: DisjunctIntervalUnion<TValue>) = this overlapWith !otherSet

  override fun isSubsetOf(largerSet: DisjunctIntervalUnion<TValue>) = (this without largerSet).isEmpty()

  override fun normalizationComparedTo(other: DisjunctIntervalUnion<TValue>) = compare(this).to(other, by = { it.leftOpen })
    .ifEqual { compare(this).to(other, by = { it.rightOpen }) }
    .ifEqual { compareEach(this.orderedBoundaries).toEach(other.orderedBoundaries) }
    .ifEqual { compareEach(this.inclusive).toEach(other.inclusive) }
    .case

  companion object {
    private val empty = DisjunctIntervalUnion<Nothing>(listOf(), booleanArrayOf(), leftOpen = false, rightOpen = false)
    private val complete = DisjunctIntervalUnion<Nothing>(listOf(), booleanArrayOf(), leftOpen = true, rightOpen = true)

    @Suppress("UNCHECKED_CAST")
    internal fun <TValue> empty(): DisjunctIntervalUnion<TValue>
      where TValue : Value,
            TValue : Comparable<TValue> = empty as DisjunctIntervalUnion<TValue>

    @Suppress("UNCHECKED_CAST")
    internal fun <TValue> complete(): DisjunctIntervalUnion<TValue>
      where TValue : Value,
            TValue : Comparable<TValue> = complete as DisjunctIntervalUnion<TValue>

    internal fun <TValue> just(value: TValue): DisjunctIntervalUnion<TValue>
      where TValue : Value, TValue : Comparable<TValue> =
      DisjunctIntervalUnion(2) { result ->
        result.appendBoundary(value, true)
        result.appendBoundary(value, true)
      }

    internal inline operator fun <TValue> invoke(
      boundaryCountEstimate: Int,
      block: (Builder<TValue>) -> Unit
    ): DisjunctIntervalUnion<TValue>
      where TValue : Value, TValue : Comparable<TValue> =
      Builder<TValue>(boundaryCountEstimate).apply(block).build()
  }

  private fun findPositionOf(value: TValue) = orderedBoundaries.binarySearch(value)

  private fun isInsideInterval(findResult: Int) = when {
    findResult > orderedBoundaries.size -> rightOpen
    findResult >= 0 ->
      // the result represents an interval boundary
      inclusive[findResult]

    else ->
      // the result represents  -insertionPoint - 1. If leftOpen, then the insertionPoint 0 (i.e. findResult -1) is inside, else, it’s outside.
      findResult % 2 == -firstOpeningIndex
  }

  private val firstOpeningIndex get() = if (leftOpen) 1 else 0
  private fun opensInterval(position: Int) = position % 2 == firstOpeningIndex
  private fun closesInterval(position: Int) = !opensInterval(position)

  internal class Builder<TValue>(boundaryCountEstimate: Int) where TValue : Value, TValue : Comparable<TValue> {
    internal var leftOpen: Boolean = false
    internal var rightOpen: Boolean = false
    private val boundariesBuilder = ArrayList<TValue>(boundaryCountEstimate)
    private val inclusiveBuilder = ArrayList<Boolean>(boundaryCountEstimate)

    fun appendBoundary(boundary: TValue, isInclusive: Boolean) {
      boundariesBuilder += boundary
      inclusiveBuilder += isInclusive
    }

    fun build() = DisjunctIntervalUnion(boundariesBuilder.apply { trimToSize() }, inclusiveBuilder.toBooleanArray(), leftOpen, rightOpen)
  }
}

private fun <TValue> exampleBetween(left: TValue?, right: TValue?): TValue = TODO()
