package software.consistent.model.value

import software.consistent.exactnumbers.*
import software.consistent.model.formatting.monospacelayout.Monospace
import software.consistent.model.formatting.monospacelayout.WithMonospaceLayout
import software.consistent.model.invariants.checkInvariant
import software.consistent.model.value.bignumbers.printFractionalNumber
import software.consistent.model.value.bignumbers.printWholeNumber
import software.consistent.model.value.number.parseNumberString
import kotlin.jvm.JvmName

public sealed interface Number : Value, Comparable<Number> {
  public fun isInteger(): Boolean

  public operator fun plus(addend: Number): Number
  public operator fun minus(subtrahend: Number): Number
  public operator fun times(factor: Number): Number
  public operator fun div(divisor: Number): Number
  public operator fun div(divisor: Int): Number = div(Number(divisor))
  public operator fun unaryMinus(): Number

  public companion object {
    @JvmName("of")
    public operator fun invoke(bigInteger: BigInteger): Number = Integer(bigInteger)

    @JvmName("of")
    public operator fun invoke(bigFraction: BigFraction): Number = fractionOrInteger(bigFraction)

    @JvmName("of")
    public operator fun invoke(byte: Byte): Number = Integer(byte.toBigInteger())

    @JvmName("ofUnsigned")
    public operator fun invoke(uByte: UByte): Number = Integer(uByte.toBigInteger())

    @JvmName("of")
    public operator fun invoke(short: Short): Number = Integer(short.toBigInteger())

    @JvmName("ofUnsigned")
    public operator fun invoke(uShort: UShort): Number = Integer(uShort.toBigInteger())

    @JvmName("of")
    public operator fun invoke(int: Int): Number = Integer(int.toBigInteger())

    @JvmName("ofUnsigned")
    public operator fun invoke(uInt: UInt): Number = Integer(uInt.toBigInteger())

    @JvmName("of")
    public operator fun invoke(long: Long): Number = Integer(long.toBigInteger())

    @JvmName("ofUnsigned")
    public operator fun invoke(uLong: ULong): Number = Integer(uLong.toBigInteger())

    @JvmName("of")
    public operator fun invoke(float: Float): Number = fractionOrInteger(float.toBigFraction())

    @JvmName("of")
    public operator fun invoke(double: Double): Number = fractionOrInteger(double.toBigFraction())

    /**
     * Parses an integer, fraction (`1/2`, `¹⁄₂`) or mixed fraction (`5 20/4`).
     * A leading `-` negates the entire number. Space separators may surround the number and sign,
     * group whole-part digits, separate the whole part from the numerator, or surround the slash.
     * The last digit group before `/` or `⁄` is the numerator; all earlier groups form the whole part.
     * Numerator digits must be consistently ordinary or raised, denominator digits consistently ordinary
     * or lowered. The fraction must be last; neither component needs to be reduced or proper.
     *
     * @throws IllegalArgumentException for invalid notation, including a zero denominator.
     * The message identifies the input, the problem and, where applicable, its one-based position.
     */
    @JvmName("of")
    public operator fun invoke(numberString: String): Number = parseNumberString(numberString)
  }
}

private fun fractionOrInteger(value: BigFraction): Number =
  if (value.isInteger()) Integer(value.numeratorAsBigInteger)
  else Fraction(value)

private class Integer(val value: BigInteger) : Number, WithMonospaceLayout {
  override fun isInteger() = true

  override fun plus(addend: Number): Number = when (addend) {
    is Integer -> this + addend
    is Fraction -> this + addend
  }

  operator fun plus(addend: Integer) = Integer(this.value + addend.value)
  operator fun plus(addend: Fraction) = Fraction(this.value.toBigFraction() + addend.value)

  override fun minus(subtrahend: Number): Number = when (subtrahend) {
    is Integer -> this - subtrahend
    is Fraction -> this - subtrahend
  }

  operator fun minus(subtrahend: Integer) = Integer(this.value - subtrahend.value)
  operator fun minus(subtrahend: Fraction) = Fraction(this.value.toBigFraction() - subtrahend.value)

  override fun times(factor: Number): Number = when (factor) {
    is Integer -> this * factor
    is Fraction -> this * factor
  }

  operator fun times(factor: Integer) = Integer(this.value * factor.value)
  operator fun times(factor: Fraction) = fractionOrInteger(this.value.toBigFraction() * factor.value)

  override fun div(divisor: Number): Number = when (divisor) {
    is Integer -> this / divisor
    is Fraction -> this / divisor
  }

  operator fun div(divisor: Integer) = fractionOrInteger(this.value / divisor.value)
  operator fun div(divisor: Fraction) = fractionOrInteger(this.value.toBigFraction() / divisor.value)

  override fun unaryMinus() = Integer(-value)

  override fun compareTo(other: Number) = when (other) {
    is Integer -> compareTo(other)
    is Fraction -> compareTo(other)
  }

  operator fun compareTo(other: Integer) = this.value.compareTo(other.value)
  operator fun compareTo(other: Fraction) = this.value.toBigFraction().compareTo(other.value)

  override val monospaceLayout get() = Monospace.atom(printWholeNumber(value))

  override fun toString() = monospaceLayout.renderToString()

  override fun equals(other: Any?) = other is Integer && value == other.value
  override fun hashCode() = value.hashCode()
}

private class Fraction(val value: BigFraction) : Number, WithMonospaceLayout {
  init {
    ::value.checkInvariant("fractions never represent integers") { !isInteger() }
  }

  override fun isInteger() = false

  override fun plus(addend: Number): Number = when (addend) {
    is Integer -> this + addend
    is Fraction -> this + addend
  }

  operator fun plus(addend: Integer) = Fraction(this.value + addend.value.toBigFraction())
  operator fun plus(addend: Fraction) = fractionOrInteger(this.value + addend.value)

  override fun minus(subtrahend: Number): Number = when (subtrahend) {
    is Integer -> this - subtrahend
    is Fraction -> this - subtrahend
  }

  operator fun minus(subtrahend: Integer) = Fraction(this.value - subtrahend.value.toBigFraction())
  operator fun minus(subtrahend: Fraction) = fractionOrInteger(this.value - subtrahend.value)

  override fun times(factor: Number): Number = when (factor) {
    is Integer -> this * factor
    is Fraction -> this * factor
  }

  operator fun times(factor: Integer) = fractionOrInteger(this.value * factor.value.toBigFraction())
  operator fun times(factor: Fraction) = fractionOrInteger(this.value * factor.value)

  override fun div(divisor: Number): Number = when (divisor) {
    is Integer -> this / divisor
    is Fraction -> this / divisor
  }

  operator fun div(divisor: Integer) = fractionOrInteger(this.value / divisor.value.toBigFraction())
  operator fun div(divisor: Fraction) = fractionOrInteger(this.value / divisor.value)

  override fun unaryMinus() = Fraction(-value)

  override fun compareTo(other: Number): Int = when (other) {
    is Integer -> compareTo(other)
    is Fraction -> compareTo(other)
  }

  operator fun compareTo(other: Integer) = this.value.compareTo(other.value.toBigFraction())
  operator fun compareTo(other: Fraction) = this.value.compareTo(other.value)

  override val monospaceLayout get() = Monospace.atom(printFractionalNumber(value))

  override fun toString() = monospaceLayout.renderToString()
  override fun equals(other: Any?) = other is Fraction && value == other.value

  override fun hashCode(): Int {
    var hash = 1
    hash = 31 * hash + value.numeratorAsBigInteger.hashCode()
    hash = 31 * hash + value.denominatorAsBigInteger.hashCode()
    hash *= value.signum()
    return hash
  }
}
