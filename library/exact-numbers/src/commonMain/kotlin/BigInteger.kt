package software.consistent.exactnumbers

public expect class BigInteger

public expect fun Byte.toBigInteger(): BigInteger
public expect fun UByte.toBigInteger(): BigInteger
public expect fun Short.toBigInteger(): BigInteger
public expect fun UShort.toBigInteger(): BigInteger
public expect fun Int.toBigInteger(): BigInteger
public expect fun UInt.toBigInteger(): BigInteger
public expect fun Long.toBigInteger(): BigInteger
public expect fun ULong.toBigInteger(): BigInteger

public expect val BigInteger.absoluteValue: BigInteger
public expect operator fun BigInteger.unaryMinus(): BigInteger

public expect operator fun BigInteger.plus(addend: BigInteger): BigInteger
public expect operator fun BigInteger.minus(subtrahend: BigInteger): BigInteger
public expect operator fun BigInteger.times(factor: BigInteger): BigInteger
public operator fun BigInteger.div(divisor: BigInteger): BigFraction = bigFractionOf(this, divisor)
// TODO verify that the callback-style API really has a benefit over returning a pair / array / custom data class. The theory is that it avoids allocations.
/**
 * Performs integer division of `this` by `divisor`, passes the resulting `quotient` and `remainder` to [resultFn] and returns whatever
 * [resultFn] returns. The `remainder`’s sign will match the sign of `quotient` (and, thus, `this`).
 *
 * Examples:
 *
 * call                           | arguments to [resultFn]
 * ------------------------------ | ----------------------
 * `10.divideAndRemainder(3) {}`  | `(3, 1)`
 * `9.divideAndRemainder(3)  {}`  | `(3, 0)`
 * `-11.divideAndRemainder(4) {}` | `(-2, -3)`
 */
public expect inline fun <R> BigInteger.divideAndRemainder(
  divisor: BigInteger,
  resultFn: (quotient: BigInteger, remainder: BigInteger) -> R
): R

public expect infix fun BigInteger.shl(bits: Int): BigInteger
public expect infix fun BigInteger.shr(bits: Int): BigInteger

public expect operator fun BigInteger.compareTo(other: BigInteger): Int

public expect object BigIntegers {
  public val ZERO: BigInteger
  public val ONE: BigInteger
  public val TWO: BigInteger
}
