package software.consistent.exactnumbers

@PublishedApi
internal fun divisionByZero(dividend: Any): ArithmeticException =
  ArithmeticException("Division by zero: attempted to divide $dividend by 0.")

internal fun divisionByZero(
  numerator: BigInteger,
  denominator: BigInteger,
): ArithmeticException = divisionByZero(if (denominator == BigIntegers.ONE) "$numerator" else "$numerator/$denominator")

internal fun zeroToNegativePower(exponent: Int) =
  ArithmeticException("Division by zero: cannot raise 0 to the negative exponent $exponent.")

internal fun nonFiniteToFraction(value: Any) =
  IllegalArgumentException("Cannot convert $value to an exact fraction: the value must be finite.")
