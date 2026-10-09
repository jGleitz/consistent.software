package software.consistent.exactnumbers

public actual typealias BigInteger = java.math.BigInteger

public actual fun Byte.toBigInteger(): BigInteger = BigInteger.valueOf(toLong())
public actual fun UByte.toBigInteger(): BigInteger = BigInteger.valueOf(toLong())
public actual fun Short.toBigInteger(): BigInteger = BigInteger.valueOf(toLong())
public actual fun UShort.toBigInteger(): BigInteger = BigInteger.valueOf(toLong())
public actual fun Int.toBigInteger(): BigInteger = BigInteger.valueOf(toLong())
public actual fun UInt.toBigInteger(): BigInteger = BigInteger.valueOf(toLong())
public actual fun Long.toBigInteger(): BigInteger = BigInteger.valueOf(this)
public actual fun ULong.toBigInteger(): BigInteger {
  val magnitude = ByteArray(ULong.SIZE_BYTES) { i ->
    (this shr ((ULong.SIZE_BYTES - 1 - i) * Byte.SIZE_BITS)).toByte()
  }
  return BigInteger(1, magnitude)
}

public actual val BigInteger.absoluteValue: BigInteger get() = abs()
public actual operator fun BigInteger.unaryMinus(): BigInteger = negate()

public actual operator fun BigInteger.plus(addend: BigInteger): BigInteger = add(addend)
public actual operator fun BigInteger.minus(subtrahend: BigInteger): BigInteger = subtract(subtrahend)
public actual operator fun BigInteger.times(factor: BigInteger): BigInteger = multiply(factor)
public actual inline fun <R> BigInteger.divideAndRemainder(
  divisor: BigInteger,
  resultFn: (quotient: BigInteger, remainder: BigInteger) -> R
): R {
  if (divisor == BigIntegers.ZERO) throw divisionByZero(this)
  return divideAndRemainder(divisor).let { (quotient, remainder) -> resultFn(quotient, remainder) }
}

public actual infix fun BigInteger.shl(bits: Int): BigInteger = shiftLeft(bits)
public actual infix fun BigInteger.shr(bits: Int): BigInteger = shiftRight(bits)

public actual operator fun BigInteger.compareTo(other: BigInteger): Int = compareTo(other)

public actual object BigIntegers {
  public actual val ZERO: BigInteger get() = BigInteger.ZERO
  public actual val ONE: BigInteger get() = BigInteger.ONE
  public actual val TWO: BigInteger get() = BigInteger.TWO
}
