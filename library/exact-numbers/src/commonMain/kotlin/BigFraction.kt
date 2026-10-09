package software.consistent.exactnumbers

public expect class BigFraction

// must not be called numerator to not collide with the synthetic property Kotlin creates for
// org.apache.commons.numbers.fraction.BigFraction#getNumerator. Quite an annoying limitation!
// Tracked in https://youtrack.jetbrains.com/issue/KT-15620
/**
 * The fraction’s numerator, including the fraction’s sign:
 *
 * `this`    | `this.numeratorAsBigInteger`
 * --------- | ---------------------------
 * `2/3`     | `2`
 * `-(8/9)`  | `-8`
 * `-(42/1)` |` -42`
 */
public expect val BigFraction.numeratorAsBigInteger: BigInteger

// must not be called denominator to not collide with the synthetic property Kotlin creates for
// org.apache.commons.numbers.fraction.BigFraction#getDenominator. Quite an annoying limitation!
// Tracked in https://youtrack.jetbrains.com/issue/KT-15620
/**
 * The fraction’s denominator, always positive:
 *
 * `this`    | `this.denominatorAsBigInteger`
 * --------- | -----------------------------
 * `2/3`     | `3`
 * `-(8/9)`  | `9`
 * `-(42/1)` | `1`
 */
public expect val BigFraction.denominatorAsBigInteger: BigInteger


/** Creates an exact, reduced fraction. The denominator must be nonzero. */
public expect fun bigFractionOf(numerator: BigInteger, denominator: BigInteger): BigFraction

public fun Byte.toBigFraction(): BigFraction = toBigInteger().toBigFraction()
public fun Short.toBigFraction(): BigFraction = toBigInteger().toBigFraction()
public fun Int.toBigFraction(): BigFraction = toBigInteger().toBigFraction()
public fun Long.toBigFraction(): BigFraction = toBigInteger().toBigFraction()
public expect fun Float.toBigFraction(): BigFraction
public expect fun Double.toBigFraction(): BigFraction
public expect fun BigInteger.toBigFraction(): BigFraction

public expect operator fun BigFraction.unaryMinus(): BigFraction

public expect operator fun BigFraction.plus(addend: BigFraction): BigFraction
public expect operator fun BigFraction.minus(subtrahend: BigFraction): BigFraction
public expect operator fun BigFraction.times(factor: BigFraction): BigFraction
public expect operator fun BigFraction.div(divisor: BigFraction): BigFraction
public expect infix fun BigFraction.toThePowerOf(exponent: Int): BigFraction

public expect operator fun BigFraction.compareTo(other: BigFraction): Int
public fun BigFraction.isInteger(): Boolean = denominatorAsBigInteger == BigIntegers.ONE

/**
 * The [signum function](https://en.wikipedia.org/wiki/Sign_function): returns `-1` if `this < 0`, `0` if `this == 0`, and, else `1` if `this > 0`.
 */
public expect fun BigFraction.signum(): Int

public expect object BigFractions {
  public val ZERO: BigFraction
  public val ONE: BigFraction
}
