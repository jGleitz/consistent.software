package software.consistent.data.type.solvers

import software.consistent.data.value.Value
import software.consistent.exactnumbers.BigIntegers
import software.consistent.exactnumbers.div
import software.consistent.exactnumbers.divideAndRemainder
import software.consistent.exactnumbers.minus
import software.consistent.exactnumbers.plus
import software.consistent.exactnumbers.times

/** Takes turns so an infinite sequence cannot hide a later sequence. */
internal fun <T> interleaveMembers(sources: List<Sequence<T>>): Sequence<T> = sequence {
  val iterators = sources.map { it.iterator() }.toMutableList()
  while (iterators.isNotEmpty()) {
    val iterator = iterators.iterator()
    while (iterator.hasNext()) {
      val source = iterator.next()
      if (source.hasNext()) yield(source.next()) else iterator.remove()
    }
  }
}

/** Every UTF-16 string, by length, with the alphabet rotated to start with a printable letter. */
internal fun symbolMembers(): Sequence<Value.Symbol> = sequence {
  yield(Value.Symbol(""))
  var digits = intArrayOf(0)
  while (true) {
    yield(Value.Symbol(digits.map { ((it + 'a'.code) % 65536).toChar() }.joinToString("")))
    var index = digits.lastIndex
    while (index >= 0 && digits[index] == 65535) {
      digits[index] = 0
      index -= 1
    }
    if (index < 0) digits = IntArray(digits.size + 1) else digits[index] += 1
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
internal fun numberMembersBetween(
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
