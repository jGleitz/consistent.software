package software.consistent.data.value.bignumbers

import software.consistent.exactnumbers.*

private const val NO_BREAK_SPACE = '\u00A0'

internal fun printWholeNumber(value: BigInteger): String = value.toString()

internal fun printFractionalNumber(value: BigFraction): String {
  val numerator = value.numeratorAsBigInteger
  val denominator = value.denominatorAsBigInteger
  val result = StringBuilder()
  if (numerator < BigIntegers.ZERO) result.append('-')

  numerator.absoluteValue.divideAndRemainder(denominator) { wholePart, fractionalNumerator ->
    if (wholePart != BigIntegers.ZERO || fractionalNumerator == BigIntegers.ZERO) {
      result.append(wholePart)
    }

    if (fractionalNumerator != BigIntegers.ZERO) {
      if (wholePart != BigIntegers.ZERO) result.append(NO_BREAK_SPACE)
      result.appendDigits(fractionalNumerator, RAISED_DIGITS)
      result.append(FRACTION_SLASH)
      result.appendDigits(denominator, LOWERED_DIGITS)
    }
  }

  return result.toString()
}

private fun StringBuilder.appendDigits(value: BigInteger, digitCharacters: String) {
  value.toString().forEach { digit -> append(digitCharacters[digit - '0']) }
}
