package software.consistent.exactnumbers

import software.consistent.exactnumbers.BigFractions.ZERO

@JsModule("fraction.js")
@JsName("default")
public external class JsFraction(
  numerator: BigInteger,
  denominator: BigInteger,
) {
  public constructor(number: Double)
  public constructor(number: BigInteger)

  @JsName("n")
  public val numerator: BigInteger

  @JsName("d")
  public val denominator: BigInteger

  @JsName("s")
  public val sign: BigInteger

  public fun neg(): JsFraction

  public fun add(other: JsFraction): JsFraction

  public fun sub(other: JsFraction): JsFraction

  public fun mul(other: JsFraction): JsFraction

  public fun div(other: JsFraction): JsFraction

  @JsName("pow")
  public fun power(exponent: Int): JsFraction

  public fun compare(other: JsFraction): Int
}

public actual typealias BigFraction = JsFraction

public actual val BigFraction.numeratorAsBigInteger: BigInteger get() = sign * numerator
public actual val BigFraction.denominatorAsBigInteger: BigInteger get() = denominator

public actual fun bigFractionOf(
  numerator: BigInteger,
  denominator: BigInteger,
): BigFraction {
  try {
    return JsFraction(numerator, denominator)
  } catch (e: Throwable) {
    throw if (e.message == JS_DIVISION_BY_ZERO_MESSAGE) divisionByZero(numerator) else e
  }
}

public actual fun Float.toBigFraction(): BigFraction = this.toDouble().toBigFraction()

public actual fun Double.toBigFraction(): BigFraction {
  if (this == 0.0) return ZERO
  if (!isFinite()) throw nonFiniteToFraction(this)

  val bits = toBits()
  val exponentField = ((bits ushr 52).toInt()) and 0x7ff
  val fractionBits = bits and 0x000f_ffff_ffff_ffffL
  val mantissa = if (exponentField == 0) fractionBits else fractionBits or (1L shl 52)

  // value = mantissa * 2^(unbiased exponent - 52)
  val unbiasedExponent = if (exponentField == 0) -1022 else exponentField - 1023
  val power = unbiasedExponent - 52
  val mantissaInteger = mantissa.toBigInteger()

  val numerator: BigInteger
  val denominator: BigInteger
  if (power >= 0) {
    numerator = mantissaInteger shl power
    denominator = BigIntegers.ONE
  } else {
    numerator = mantissaInteger
    denominator = BigIntegers.ONE shl -power
  }

  val signedNumerator = if (bits < 0) -numerator else numerator
  // Fraction.js reduces the numerator and denominator in its constructor.
  return bigFractionOf(signedNumerator, denominator)
}

public actual fun BigInteger.toBigFraction(): BigFraction = bigFractionOf(this, BigIntegers.ONE)

public actual operator fun BigFraction.unaryMinus(): BigFraction = neg()

public actual operator fun BigFraction.plus(addend: BigFraction): BigFraction = add(addend)

public actual operator fun BigFraction.minus(subtrahend: BigFraction): BigFraction = sub(subtrahend)

public actual operator fun BigFraction.times(factor: BigFraction): BigFraction = mul(factor)

public actual operator fun BigFraction.div(divisor: BigFraction): BigFraction {
  try {
    return div(divisor)
  } catch (e: Throwable) {
    throw if (e.message == JS_DIVISION_BY_ZERO_MESSAGE) divisionByZero(numeratorAsBigInteger, denominatorAsBigInteger) else e
  }
}

public actual infix fun BigFraction.toThePowerOf(exponent: Int): BigFraction {
  if (exponent < 0 && numeratorAsBigInteger == BigIntegers.ZERO) throw zeroToNegativePower(exponent)
  return power(exponent)
}

public actual operator fun BigFraction.compareTo(other: BigFraction): Int = compare(other)

// fraction.js gives zero the sign 1.
public actual fun BigFraction.signum(): Int = if (numerator == BigIntegers.ZERO) 0 else sign.asInt()

public actual object BigFractions {
  // we trust the runtime environment to apply interning if deems it necessary
  public actual val ZERO: BigFraction = BigFraction(BigIntegers.ZERO)
  public actual val ONE: BigFraction = BigFraction(BigIntegers.ONE)
}

private const val JS_DIVISION_BY_ZERO_MESSAGE = "Division by Zero"
