package software.consistent.data.type.solvers

import software.consistent.data.type.solvers.generic.DisjunctIntervalUnion
import software.consistent.data.value.Value
import software.consistent.exactnumbers.BigIntegers
import software.consistent.exactnumbers.div
import software.consistent.exactnumbers.divideAndRemainder
import software.consistent.exactnumbers.minus
import software.consistent.exactnumbers.plus
import software.consistent.exactnumbers.times
import software.consistent.formatting.monospacelayout.Monospace

internal class NumberSet
private constructor(
    orderedBoundaries: List<Value.Number>,
    inclusive: BooleanArray,
    leftOpen: Boolean,
    rightOpen: Boolean,
) :
    DisjunctIntervalUnion<Value.Number, NumberSet>(
        orderedBoundaries,
        inclusive,
        leftOpen,
        rightOpen,
    ) {
  override fun construct(
      orderedBoundaries: List<Value.Number>,
      inclusive: BooleanArray,
      leftOpen: Boolean,
      rightOpen: Boolean,
  ) = NumberSet(orderedBoundaries, inclusive, leftOpen, rightOpen)

  override fun exampleBetween(left: Value.Number?, right: Value.Number?): Value.Number =
      when {
        left == null && right == null -> Value.Number(0)
        left == null -> right!! - Value.Number(1)
        right == null -> left + Value.Number(1)
        else -> (left + right) / 2
      }

  override fun membersBetween(left: Value.Number?, right: Value.Number?) =
      numberMembersBetween(left, right)

  override val completeLayout
    get() = Monospace.atom("AnyNumber")

  companion object {
    private val empty =
        NumberSet(emptyList(), booleanArrayOf(), leftOpen = false, rightOpen = false)
    private val complete =
        NumberSet(emptyList(), booleanArrayOf(), leftOpen = true, rightOpen = true)

    fun empty() = empty

    fun complete() = complete

    fun just(value: Value.Number) =
        NumberSet(listOf(value, value), booleanArrayOf(true, true), false, false)

    operator fun invoke(
        boundaryCountEstimate: Int,
        block: (Builder<Value.Number, NumberSet>) -> Unit,
    ): NumberSet =
        Builder<Value.Number, NumberSet>(boundaryCountEstimate, ::NumberSet).apply(block).build()
  }
}

/**
 * The Calkin–Wilf recurrence visits each positive rational exactly once, using constant storage.
 */
private fun positiveRationals(): Sequence<Value.Number> = sequence {
  var numerator = BigIntegers.ONE
  var denominator = BigIntegers.ONE
  while (true) {
    yield(Value.Number(numerator / denominator))
    val quotient = numerator.divideAndRemainder(denominator) { quotient, _ -> quotient }
    val nextDenominator = (BigIntegers.TWO * quotient + BigIntegers.ONE) * denominator - numerator
    numerator = denominator
    denominator = nextDenominator
  }
}

/** Rational bijections into open intervals avoid scanning numbers outside narrow intervals. */
private fun numberMembersBetween(
    left: Value.Number?,
    right: Value.Number?,
): Sequence<Value.Number> = sequence {
  if (left == null && right == null) yield(Value.Number(0))
  for (positive in positiveRationals()) {
    when {
      left == null && right == null -> {
        yield(positive)
        yield(-positive)
      }
      left == null -> yield(right!! - positive)
      right == null -> yield(left + positive)
      else -> yield(left + (right - left) * positive / (Value.Number(1) + positive))
    }
  }
}
