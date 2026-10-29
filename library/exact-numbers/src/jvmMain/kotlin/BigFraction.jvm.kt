package software.consistent.exactnumbers

public actual typealias BigFraction = org.apache.commons.numbers.fraction.BigFraction

// Commons Numbers keeps the sign wherever an operation leaves it (e.g. 4 ÷ -2 = 2/-1), so normalise it on access.
public actual val BigFraction.numeratorAsBigInteger: BigInteger
  get() = if (denominator.signum() < 0) numerator.negate() else numerator
public actual val BigFraction.denominatorAsBigInteger: BigInteger get() = denominator.abs()

public actual fun bigFractionOf(numerator: BigInteger, denominator: BigInteger): BigFraction {
  if (denominator.signum() == 0) throw divisionByZero(numerator)

  return if (denominator.signum() < 0) BigFraction.of(numerator.negate(), denominator.negate())
  else BigFraction.of(numerator, denominator)
}

public actual fun Float.toBigFraction(): BigFraction = toDouble().toBigFraction()
public actual fun Double.toBigFraction(): BigFraction {
  if (!isFinite()) throw nonFiniteToFraction(this)
  return BigFraction.from(this)
}

public actual fun BigInteger.toBigFraction(): BigFraction = BigFraction.of(this)

public actual operator fun BigFraction.unaryMinus(): BigFraction = negate()

public actual operator fun BigFraction.plus(addend: BigFraction): BigFraction = add(addend)
public actual operator fun BigFraction.minus(subtrahend: BigFraction): BigFraction = subtract(subtrahend)
public actual operator fun BigFraction.times(factor: BigFraction): BigFraction = multiply(factor)
public actual operator fun BigFraction.div(divisor: BigFraction): BigFraction {
  if (divisor == ZERO) throw divisionByZero(numeratorAsBigInteger, denominatorAsBigInteger)
  return divide(divisor)
}

public actual infix fun BigFraction.toThePowerOf(exponent: Int): BigFraction {
  if (exponent < 0 && numerator == BigIntegers.ZERO) throw zeroToNegativePower(exponent)
  return pow(exponent)
}

public actual operator fun BigFraction.compareTo(other: BigFraction): Int = compareTo(other)

public actual fun BigFraction.signum(): Int = signum()

public actual object BigFractions {
  public actual val ZERO: BigFraction get() = BigFraction.ZERO
  public actual val ONE: BigFraction get() = BigFraction.ONE
}
