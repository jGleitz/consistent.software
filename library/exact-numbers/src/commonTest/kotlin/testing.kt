package software.consistent.exactnumbers

import ch.tutteli.atrium.creating.Expect
import ch.tutteli.atrium.logic._logic
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.arbitrary.*
import io.kotest.property.exhaustive.exhaustive

internal val twoToThe63: BigInteger = (1uL shl 63).toBigInteger()

internal fun twoToThe(exponent: Int): BigInteger {
  var result = BigIntegers.ONE
  var base = BigIntegers.TWO
  var remaining = exponent
  while (remaining > 0) {
    if (remaining and 1 == 1) result *= base
    base *= base
    remaining = remaining shr 1
  }
  return result
}

/** The exact rational value of a finite [double], computed from its IEEE 754 bits. Serves as oracle for conversions. */
internal fun exactValueOf(double: Double): BigFraction {
  require(double.isFinite()) { "$double has no exact rational value" }
  val bits = double.toRawBits()
  val biasedExponent = ((bits ushr 52) and 0x7FF).toInt()
  val storedSignificand = bits and 0xF_FFFF_FFFF_FFFFL
  val isSubnormal = biasedExponent == 0
  val significand = if (isSubnormal) storedSignificand else storedSignificand or (1L shl 52)
  val exponent = (if (isSubnormal) 1 else biasedExponent) - 1075
  val magnitude =
    if (exponent >= 0) {
      (significand.toBigInteger() * twoToThe(exponent)).toBigFraction()
    } else {
      bigFractionOf(significand.toBigInteger(), twoToThe(-exponent))
    }
  return if (bits < 0) -magnitude else magnitude
}

/** Integers of any sign: mostly beyond 64 bits, but also within the [Long] range (including its extremes). */
internal fun Arb.Companion.bigInteger(): Arb<BigInteger> =
  choice(
    long().map { it.toBigInteger() },
    bind(long(), long(), long()) { a, b, c -> a.toBigInteger() * b.toBigInteger() + c.toBigInteger() },
  )

/** All [UByte]s from [from] to [to]; the unsigned counterpart of [io.kotest.property.exhaustive.bytes]. */
internal fun Exhaustive.Companion.uBytes(
  from: UByte = UByte.MIN_VALUE,
  to: UByte = UByte.MAX_VALUE,
): Exhaustive<UByte> = (from..to).map { it.toUByte() }.exhaustive()

internal fun Arb.Companion.nonZeroBigInteger(): Arb<BigInteger> = bigInteger().filter { it.compareTo(BigIntegers.ZERO) != 0 }

/**
 * A fraction together with the (unreduced) numerator and denominator it was created from, so tests can compute expected
 * results by the textbook definitions. Denominators take both signs on purpose, to cover sign normalization everywhere.
 */
internal data class Ratio(
  val numerator: BigInteger,
  val denominator: BigInteger,
) {
  val value: BigFraction get() = bigFractionOf(numerator, denominator)

  override fun toString() = "$numerator/$denominator"
}

internal fun Arb.Companion.ratio(): Arb<Ratio> = bind(bigInteger(), nonZeroBigInteger(), ::Ratio)

/**
 * Asserts equality by [compareTo] instead of [equals], so arithmetic tests do not depend on [equals] being implemented
 * correctly (which is tested separately).
 */
internal fun Expect<BigFraction>.toEqualNumerically(expected: BigFraction) =
  _logic.createAndAppend("to equal numerically", expected) { it.compareTo(expected) == 0 }
