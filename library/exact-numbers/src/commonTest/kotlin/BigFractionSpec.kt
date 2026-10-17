package software.consistent.exactnumbers

import ch.tutteli.atrium.api.fluent.en_GB.*
import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.test.Enabled.Companion.disabled
import io.kotest.datatest.withData
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlin.math.floor
import kotlin.math.sign

class BigFractionSpec : DescribeSpec({
  describe("BigFraction") {
    describe("bigFractionOf") {
      it("supports values beyond JavaScript number's safe-integer limits") {
        val beyondMaxSafeInteger = twoToThe(53) + BigIntegers.ONE

        expect(bigFractionOf(beyondMaxSafeInteger, BigIntegers.ONE)) {
          feature(BigFraction::numeratorAsBigInteger).toEqual(beyondMaxSafeInteger)
          feature(BigFraction::denominatorAsBigInteger).toEqual(BigIntegers.ONE)
        }
        expect(bigFractionOf(BigIntegers.ONE, beyondMaxSafeInteger)) {
          feature(BigFraction::numeratorAsBigInteger).toEqual(BigIntegers.ONE)
          feature(BigFraction::denominatorAsBigInteger).toEqual(beyondMaxSafeInteger)
        }
      }

      describe("rejects a zero denominator") {
        withData(listOf(
          BigIntegers.ZERO, BigIntegers.ONE, -BigIntegers.ONE, 42.toBigInteger(), (-42).toBigInteger(),
          twoToThe(100), -twoToThe(100),
        )) { numerator ->
          expect { bigFractionOf(numerator, BigIntegers.ZERO) }.toThrow<ArithmeticException>().and.message.toEqual(
            "Division by zero: attempted to divide $numerator by 0."
          )
        }
      }
    }

    describe("toBigFraction") {
      it("converts to the fraction with denominator one") {
        for (integer in fractionBoundaryIntegers) {
          expect(integer.toBigFraction()).toEqualNumerically(bigFractionOf(integer, BigIntegers.ONE))
        }
        checkAll(PropTestConfig(iterations = 100), Arb.bigInteger()) { integer ->
          expect(integer.toBigFraction()).toEqualNumerically(bigFractionOf(integer, BigIntegers.ONE))
        }
      }

      // Random bit patterns cover all exponents (including subnormals) evenly, unlike random values from a range.
      it("converts every finite double to exactly the value it represents") {
        checkAll(Arb.long().map { Double.fromBits(it) }.filter { it.isFinite() }) { double ->
          expect(double.toBigFraction()).toEqualNumerically(exactValueOf(double))
        }
      }

      // Independent ground truth for the oracle used above, for the kinds of values most likely to be mishandled.
      describe("converts doubles to their exact binary value") {
        withData(
          mapOf(
            "0.1 (not 1/10)" to (0.1 to bigFractionOf(3602879701896397L.toBigInteger(), twoToThe(55))),
            "1.0 / 3.0 (not 1/3)" to ((1.0 / 3.0) to bigFractionOf(6004799503160661L.toBigInteger(), twoToThe(54))),
            "-2.5" to (-2.5 to bigFractionOf((-5).toBigInteger(), 2.toBigInteger())),
            "1e23 (not 10^23)" to
              (1e23 to (99_999_999L.toBigInteger() * 1_000_000_000_000_000L.toBigInteger() +
                999_999_991_611_392L.toBigInteger()).toBigFraction()),
            "Double.MAX_VALUE" to (Double.MAX_VALUE to ((twoToThe(53) - BigIntegers.ONE) * twoToThe(971)).toBigFraction()),
            "Double.MIN_VALUE" to (Double.MIN_VALUE to bigFractionOf(BigIntegers.ONE, twoToThe(1074))),
          )
        ) { (double, expected) ->
          expect(double.toBigFraction()).toEqualNumerically(expected)
        }
      }

      // Negative zero is the only double whose sign is not reflected in its value.
      it("converts negative zero to the same fraction as zero") {
        val negativeZero = (-0.0).toBigFraction()
        val zeroFraction = BigIntegers.ZERO.toBigFraction()
        expect(negativeZero).toEqual(zeroFraction)
      }

      describe("rejects non-finite doubles") {
        withData(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY) { double ->
          expect { double.toBigFraction() }.toThrow<IllegalArgumentException>().and.message.toEqual(
            "Cannot convert $double to an exact fraction: the value must be finite."
          )
        }
      }
      it("converts every finite float to exactly the value it represents") {
        checkAll(Arb.int().map { Float.fromBits(it) }.filter { it.isFinite() }) { float ->
          expect(float.toBigFraction()).toEqualNumerically(exactValueOf(float.toDouble()))
        }
      }

      it("converts 0.1f to its exact binary value (not 1/10)") {
        expect(Float.fromBits(0.1f.toBits()).toBigFraction()).toEqualNumerically(bigFractionOf(13421773.toBigInteger(), twoToThe(27)))
      }

      describe("rejects non-finite floats") {
        withData(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY) { float ->
          expect { float.toBigFraction() }.toThrow<IllegalArgumentException>().and.message.toEqual(
            "Cannot convert $float to an exact fraction: the value must be finite."
          )
        }
      }
    }

    describe("arithmetic") {
      // The expected results follow the textbook definitions on the unreduced numerators and denominators.
      describe("unaryMinus") {
        it("negates") {
          for (ratio in boundaryRatios) {
            expect(-ratio.value).toEqualNumerically(bigFractionOf(-ratio.numerator, ratio.denominator))
          }
          checkAll(PropTestConfig(iterations = 100), Arb.ratio()) { ratio ->
            expect(-ratio.value).toEqualNumerically(bigFractionOf(-ratio.numerator, ratio.denominator))
          }
        }
      }

      describe("plus") {
        it("adds") {
          checkAll(Arb.ratio(), Arb.ratio()) { left, right ->
            expect(left.value + right.value).toEqualNumerically(
              bigFractionOf(
                left.numerator * right.denominator + right.numerator * left.denominator,
                left.denominator * right.denominator,
              )
            )
          }
        }
      }

      describe("minus") {
        it("subtracts") {
          checkAll(Arb.ratio(), Arb.ratio()) { left, right ->
            expect(left.value - right.value).toEqualNumerically(
              bigFractionOf(
                left.numerator * right.denominator - right.numerator * left.denominator,
                left.denominator * right.denominator,
              )
            )
          }
        }
      }

      describe("times") {
        it("multiplies") {
          checkAll(Arb.ratio(), Arb.ratio()) { left, right ->
            expect(left.value * right.value).toEqualNumerically(
              bigFractionOf(left.numerator * right.numerator, left.denominator * right.denominator)
            )
          }
        }
      }

      describe("div") {
        it("divides") {
          checkAll(Arb.ratio(), Arb.ratio().filter { it.numerator.compareTo(BigIntegers.ZERO) != 0 }) { dividend, divisor ->
            expect(dividend.value / divisor.value).toEqualNumerically(
              bigFractionOf(dividend.numerator * divisor.denominator, dividend.denominator * divisor.numerator)
            )
          }
        }

        describe("rejects division by zero") {
          withData(listOf(
            (0 to 1) to "0",
            (0 to -7) to "0",
            (42 to 1) to "42",
            (-42 to 1) to "-42",
            (6 to 3) to "2",
            (1 to 2) to "1/2",
            (-1 to 2) to "-1/2",
            (7 to 3) to "7/3",
            (-7 to 3) to "-7/3",
            (4 to -6) to "-2/3",
            (-4 to -6) to "2/3",
            (14 to 6) to "7/3",
          )) { (input, dividendText) ->
            val value = bigFractionOf(input.first.toBigInteger(), input.second.toBigInteger())
            expect { value / BigFractions.ZERO }.toThrow<ArithmeticException>().and.message.toEqual(
              "Division by zero: attempted to divide $dividendText by 0."
            )
          }
        }
      }

      describe("toThePowerOf") {
        // Repeated integer multiplication gives an independent oracle without using either backend's pow.
        // Bound the exponents so generated large numerators and denominators remain cheap to raise to a power.
        it("raises the numerator and denominator to positive powers") {
          checkAll(PropTestConfig(iterations = 200), Arb.ratio(), Arb.int(1..10)) { ratio, exponent ->
            var numerator = BigIntegers.ONE
            var denominator = BigIntegers.ONE
            repeat(exponent) {
              numerator *= ratio.numerator
              denominator *= ratio.denominator
            }
            expect(ratio.value.toThePowerOf(exponent)).toEqualNumerically(bigFractionOf(numerator, denominator))
          }
        }

        it("raises the reciprocal to negative powers") {
          checkAll(PropTestConfig(iterations = 200), Arb.ratio().filter { it.numerator.compareTo(BigIntegers.ZERO) != 0 }, Arb.int(1..10)) { ratio, exponent ->
            var numerator = BigIntegers.ONE
            var denominator = BigIntegers.ONE
            repeat(exponent) {
              numerator *= ratio.denominator
              denominator *= ratio.numerator
            }
            expect(ratio.value.toThePowerOf(-exponent)).toEqualNumerically(bigFractionOf(numerator, denominator))
          }
        }

        describe("returns one for exponent zero, including zero to the zeroth power") {
          withData(listOf(
            0 to 1, 1 to 1, -1 to 1, 42 to 1, -42 to 1,
            1 to 2, -1 to 2, 7 to 3, -7 to 3, 4 to -6, -4 to -6,
          )) { (numerator, denominator) ->
            expect(bigFractionOf(numerator.toBigInteger(), denominator.toBigInteger()).toThePowerOf(0))
              .toEqualNumerically(BigFractions.ONE)
          }
          it("supports values beyond Long") {
            expect(bigFractionOf(twoToThe(100), -twoToThe(64)).toThePowerOf(0)).toEqualNumerically(BigFractions.ONE)
          }
        }

        it("returns the base for exponent one") {
          for (ratio in boundaryRatios) {
            expect(ratio.value.toThePowerOf(1)).toEqualNumerically(ratio.value)
          }
          checkAll(PropTestConfig(iterations = 100), Arb.ratio()) { ratio ->
            expect(ratio.value.toThePowerOf(1)).toEqualNumerically(ratio.value)
          }
        }

        describe("preserves a negative base's sign only for odd exponents") {
          withData(
            mapOf(
              "positive odd exponent" to (3 to bigFractionOf((-8).toBigInteger(), 27.toBigInteger())),
              "positive even exponent" to (2 to bigFractionOf(4.toBigInteger(), 9.toBigInteger())),
              "negative odd exponent" to (-3 to bigFractionOf((-27).toBigInteger(), 8.toBigInteger())),
              "negative even exponent" to (-2 to bigFractionOf(9.toBigInteger(), 4.toBigInteger())),
            )
          ) { (exponent, expected) ->
            // An unreduced fraction with a negative denominator also exercises reduction and sign normalization.
            val base = bigFractionOf(4.toBigInteger(), (-6).toBigInteger())
            expect(base.toThePowerOf(exponent)).toEqualNumerically(expected)
          }
        }

        describe("raises zero to positive powers") {
          withData(1, 2, 3, 7, 8, 9, 31, 32, 33, 63, 64, 65, Int.MAX_VALUE - 1, Int.MAX_VALUE) { exponent ->
            expect(BigFractions.ZERO.toThePowerOf(exponent)).toEqualNumerically(BigFractions.ZERO)
          }
        }

        describe("rejects negative powers of zero") {
          withData(Int.MIN_VALUE, Int.MIN_VALUE + 1, -2, -1) { exponent ->
            expect { BigFractions.ZERO.toThePowerOf(exponent) }.toThrow<ArithmeticException>().and.message.toEqual(
              "Division by zero: cannot raise 0 to the negative exponent $exponent."
            )
          }
        }

        it("keeps powers exact beyond floating-point precision and range") {
          val base = bigFractionOf(BigIntegers.TWO, 3.toBigInteger())
          var threeToThe1100 = BigIntegers.ONE
          repeat(1100) { threeToThe1100 *= 3.toBigInteger() }
          expect(base.toThePowerOf(1100)).toEqualNumerically(bigFractionOf(twoToThe(1100), threeToThe1100))
          expect(base.toThePowerOf(-1100)).toEqualNumerically(bigFractionOf(threeToThe1100, twoToThe(1100)))
        }

        // Unit bases keep the result small even at the Int limits, including MIN_VALUE whose negation overflows.
        describe("supports extreme exponents for unit bases") {
          withData(Int.MIN_VALUE, Int.MIN_VALUE + 1, Int.MAX_VALUE - 1, Int.MAX_VALUE) { exponent ->
            expect(BigFractions.ONE.toThePowerOf(exponent)).toEqualNumerically(BigFractions.ONE)
            expect((-BigFractions.ONE).toThePowerOf(exponent)).toEqualNumerically(
              if (exponent % 2 == 0) BigFractions.ONE else -BigFractions.ONE
            )
          }
        }
      }
    }

    describe("compareTo") {
      // a/b - c/d = (ad - cb) / bd, so the sign of the difference is the sign of (ad - cb) * bd.
      it("orders by value") {
        checkAll(Arb.ratio(), Arb.ratio()) { left, right ->
          val differenceSign =
            ((left.numerator * right.denominator - right.numerator * left.denominator) *
              (left.denominator * right.denominator)).compareTo(BigIntegers.ZERO).sign
          expect(left.value.compareTo(right.value).sign).toEqual(differenceSign)
        }
      }

      // Random pairs of fractions are practically never equal, so equal values need their own test.
      it("considers differently represented equal values equal") {
        checkAll(PropTestConfig(iterations = 200), Arb.ratio(), Arb.nonZeroBigInteger()) { ratio, factor ->
          val expanded = bigFractionOf(ratio.numerator * factor, ratio.denominator * factor)
          expect(expanded.compareTo(ratio.value)).toEqual(0)
        }
      }

      // Random pairs of fractions are practically never this close, so tiny differences need their own test.
      it("distinguishes values that differ by far less than double precision") {
        checkAll(PropTestConfig(iterations = 200), Arb.ratio(), Arb.nonZeroBigInteger()) { ratio, integer ->
          val tinyPositive = bigFractionOf(BigIntegers.ONE, integer * integer * twoToThe(1100))
          expect(ratio.value.compareTo(ratio.value + tinyPositive).sign).toEqual(-1)
        }
      }
    }

    // The exact integers are exactly the reduced fractions with denominator one, so these tests also check that every
    // way of creating a fraction yields a reduced one.
    describe("isInteger") {
      it("recognises integers created from numerator and denominator") {
        checkAll(Arb.int(), Arb.int().filter { it != 0 }, Arb.int(-2..2)) { quotient, denominator, remainder ->
          val numerator = quotient.toLong() * denominator + remainder
          expect(bigFractionOf(numerator.toBigInteger(), denominator.toBigInteger()).isInteger())
            .toEqual(numerator % denominator == 0L)
        }
      }

      it("recognises integers created from doubles") {
        checkAll(Arb.long().map { Double.fromBits(it) }.filter { it.isFinite() }) { double ->
          expect(double.toBigFraction().isInteger()).toEqual(floor(double) == double)
        }
      }

      it("recognises integers resulting from arithmetic") {
        checkAll(Arb.ratio()) { ratio ->
          expect((ratio.value * ratio.denominator.toBigFraction()).isInteger()).toEqual(true)
        }
      }
    }

    describe("equals & hashCode") {
      it("considers differently represented equal values equal") {
        checkAll(PropTestConfig(iterations = 200), Arb.ratio(), Arb.nonZeroBigInteger()) { ratio, factor ->
          expect(bigFractionOf(ratio.numerator * factor, ratio.denominator * factor)).toEqual(ratio.value)
        }
      }

      it("gives differently represented equal values the same hash code")
        .config(enabledOrReasonIf = { disabled("not possible on JS while using value classes") }) {
          expect(0.0.toBigFraction()).feature(Any::hashCode).toEqual(BigFractions.ZERO.hashCode())
          expect((-0.0).toBigFraction()).feature(Any::hashCode).toEqual(BigFractions.ZERO.hashCode())
          checkAll(Arb.ratio(), Arb.nonZeroBigInteger()) { ratio, factor ->
            expect(bigFractionOf(ratio.numerator * factor, ratio.denominator * factor))
              .feature(BigFraction::hashCode)
              .toEqual(ratio.value.hashCode())
          }
        }

      it("agrees with compareTo") {
        for (left in boundaryRatios) {
          for (right in boundaryRatios) {
            expect(left.value == right.value).toEqual(left.value.compareTo(right.value) == 0)
          }
        }
        checkAll(PropTestConfig(iterations = 100), Arb.ratio(), Arb.ratio()) { left, right ->
          expect(left.value == right.value).toEqual(left.value.compareTo(right.value) == 0)
        }
      }
    }
    describe("explicit boundaries for the sampled properties") {
      it("raises signed and normalised bases at the exponent range endpoints and neighbours") {
        for (ratio in boundaryRatios) {
          for (exponent in listOf(1, 2, 9, 10)) {
            var numerator = BigIntegers.ONE
            var denominator = BigIntegers.ONE
            repeat(exponent) {
              numerator *= ratio.numerator
              denominator *= ratio.denominator
            }
            expect(ratio.value.toThePowerOf(exponent)).toEqualNumerically(bigFractionOf(numerator, denominator))
            if (ratio.numerator != BigIntegers.ZERO) {
              expect(ratio.value.toThePowerOf(-exponent)).toEqualNumerically(bigFractionOf(denominator, numerator))
            }
          }
        }
      }

      it("compares and equates fractions expanded by signed unit and large factors") {
        for (ratio in boundaryRatios) {
          for (factor in fractionBoundaryIntegers.filter { it != BigIntegers.ZERO }) {
            val expanded = bigFractionOf(ratio.numerator * factor, ratio.denominator * factor)
            expect(expanded.compareTo(ratio.value)).toEqual(0)
            expect(expanded).toEqual(ratio.value)
            val tinyPositive = bigFractionOf(BigIntegers.ONE, factor * factor * twoToThe(1100))
            expect(ratio.value.compareTo(ratio.value + tinyPositive).sign).toEqual(-1)
          }
        }
      }
    }
  }
})

private val fractionBoundaryIntegers = listOf(
  BigIntegers.ZERO, BigIntegers.ONE, -BigIntegers.ONE,
  Long.MIN_VALUE.toBigInteger(), (Long.MIN_VALUE + 1).toBigInteger(),
  (Long.MAX_VALUE - 1).toBigInteger(), Long.MAX_VALUE.toBigInteger(),
  twoToThe(100), -twoToThe(100),
)

private val boundaryRatios =
  listOf(0 to 1, 1 to 1, -1 to 1, 1 to 2, -1 to 2, 7 to 3, -7 to 3, 4 to -6, -4 to -6)
    .map { (numerator, denominator) -> Ratio(numerator.toBigInteger(), denominator.toBigInteger()) } +
    listOf(
      Ratio(Long.MIN_VALUE.toBigInteger(), Long.MAX_VALUE.toBigInteger()),
      Ratio(twoToThe(100) + BigIntegers.ONE, -twoToThe(100)),
    )
