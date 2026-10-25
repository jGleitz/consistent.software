package software.consistent.data.value

import ch.tutteli.atrium.api.fluent.en_GB.*
import ch.tutteli.atrium.api.verbs.expect
import ch.tutteli.atrium.creating.Expect
import ch.tutteli.atrium.logic._logic
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withIts
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.bytes
import io.kotest.property.exhaustive.enum
import io.kotest.property.exhaustive.ints
import software.consistent.data.generators.uBytes
import software.consistent.exactnumbers.*
import kotlin.math.sign

@OptIn(ExperimentalWithOptions::class)
class NumberSpec : DescribeSpec({
  describe("Number") {
    describe("creation from exact numbers") {
      it("preserves BigInteger values, including zero, signs and values beyond the Long range") {
        val large = BigIntegers.ONE shl 100
        val largeNumber = Number("1267650600228229401496703205376")
        for ((input, expected) in listOf(
          BigIntegers.ZERO to Number(0),
          Long.MIN_VALUE.toBigInteger() to Number(Long.MIN_VALUE),
          Long.MAX_VALUE.toBigInteger() to Number(Long.MAX_VALUE),
          large to largeNumber,
          -large to -largeNumber,
        )) {
          val actual = Number(input)
          expect(actual).toEqual(expected)
          expect(actual.hashCode()).toEqual(expected.hashCode())
          expect(actual.isInteger()).toEqual(true)
        }
        for (long in longBoundaries) {
          expect(Number(long.toBigInteger())).toEqual(Number(long))
        }
        checkAll(PropTestConfig(iterations = 200), Arb.long()) { long ->
          expect(Number(long.toBigInteger())).toEqual(Number(long))
        }
      }

      it("creates canonical integers from integral BigFractions") {
        val large = BigIntegers.ONE shl 100
        for ((input, expected) in listOf(
          BigFractions.ZERO to Number(0),
          BigFractions.ONE to Number(1),
          bigFractionOf(0.toBigInteger(), 7.toBigInteger()) to Number(0),
          bigFractionOf(20.toBigInteger(), 4.toBigInteger()) to Number(5),
          bigFractionOf(20.toBigInteger(), (-4).toBigInteger()) to Number(-5),
          bigFractionOf(large * BigIntegers.TWO, BigIntegers.TWO) to Number("1267650600228229401496703205376"),
        )) {
          val actual = Number(input)
          expect(actual).toEqual(expected)
          expect(actual.hashCode()).toEqual(expected.hashCode())
          expect(actual.isInteger()).toEqual(true)
        }
      }

      it("preserves proper, improper, reduced and signed BigFractions") {
        for ((numerator, denominator) in listOf(1 to 2, 7 to 3, 2 to 4, -7 to 3, 7 to -3, -7 to -3)) {
          val actual = Number(bigFractionOf(numerator.toBigInteger(), denominator.toBigInteger()))
          val expected = Number(numerator) / Number(denominator)
          expect(actual).toEqual(expected)
          expect(actual.hashCode()).toEqual(expected.hashCode())
          expect(actual.isInteger()).toEqual(false)
        }
        checkAll(Arb.long(), Arb.long().filter { it != 0L }) { numerator, denominator ->
          val actual = Number(bigFractionOf(numerator.toBigInteger(), denominator.toBigInteger()))
          val expected = Number(numerator) / Number(denominator)
          expect(actual).toEqual(expected)
          expect(actual.isInteger()).toEqual(expected.isInteger())
        }
      }

      it("preserves BigFractions with numerators and denominators beyond the Long range") {
        val large = BigIntegers.ONE shl 100
        val largeNumber = Number("1267650600228229401496703205376")
        for (sign in listOf(-1, 1)) {
          val numerator = (large + BigIntegers.ONE) * sign.toBigInteger()
          val actual = Number(bigFractionOf(numerator, large))
          val expected = (largeNumber + Number(1)) * Number(sign) / largeNumber
          expect(actual).toEqual(expected)
          expect(actual.isInteger()).toEqual(false)
          expect(actual * largeNumber).toEqual((largeNumber + Number(1)) * Number(sign))
        }
      }
    }

    describe("creation from primitives") {
      it("can be created from bytes") {
        checkAll(Exhaustive.bytes()) { byte ->
          expect(Number(byte)).feature(Number::toString).toEqual(byte.toString())
        }
      }

      it("can be created from uBytes") {
        checkAll(Exhaustive.uBytes()) { uByte ->
          expect(Number(uByte)).feature(Number::toString).toEqual(uByte.toString())
        }
      }

      it("can be created from shorts") {
        for (short in listOf(Short.MIN_VALUE, (Short.MIN_VALUE + 1).toShort(), (-1).toShort(), 0.toShort(), 1.toShort(), (Short.MAX_VALUE - 1).toShort(), Short.MAX_VALUE)) {
          expect(Number(short)).feature(Number::toString).toEqual(short.toString())
        }
        checkAll(PropTestConfig(iterations = 100), Arb.short()) { short ->
          expect(Number(short)).feature(Number::toString).toEqual(short.toString())
        }
      }

      it("can be created from uShorts") {
        for (uShort in listOf(0u, 1u, 32767u, 32768u, 32769u, 65534u, 65535u).map { it.toUShort() }) {
          expect(Number(uShort)).feature(Number::toString).toEqual(uShort.toString())
        }
        checkAll(PropTestConfig(iterations = 100), Arb.uShort()) { uShort ->
          expect(Number(uShort)).feature(Number::toString).toEqual(uShort.toString())
        }
      }

      it("can be created from ints") {
        for (int in intBoundaries) {
          expect(Number(int)).feature(Number::toString).toEqual(int.toString())
        }
        checkAll(PropTestConfig(iterations = 200), Arb.int()) { int ->
          expect(Number(int)).feature(Number::toString).toEqual(int.toString())
        }
      }

      it("can be created from uInts") {
        for (uInt in listOf(0u, 1u, Int.MAX_VALUE.toUInt(), Int.MAX_VALUE.toUInt() + 1u, Int.MAX_VALUE.toUInt() + 2u, UInt.MAX_VALUE - 1u, UInt.MAX_VALUE)) {
          expect(Number(uInt)).feature(Number::toString).toEqual(uInt.toString())
        }
        checkAll(PropTestConfig(iterations = 200), Arb.uInt()) { uInt ->
          expect(Number(uInt)).feature(Number::toString).toEqual(uInt.toString())
        }
      }

      it("can be created from longs") {
        for (long in longBoundaries) {
          expect(Number(long)).feature(Number::toString).toEqual(long.toString())
        }
        checkAll(PropTestConfig(iterations = 200), Arb.long()) { long ->
          expect(Number(long)).feature(Number::toString).toEqual(long.toString())
        }
      }

      it("can be created from uLongs") {
        for (uLong in listOf(0uL, 1uL, Long.MAX_VALUE.toULong(), Long.MAX_VALUE.toULong() + 1uL, Long.MAX_VALUE.toULong() + 2uL, ULong.MAX_VALUE - 1uL, ULong.MAX_VALUE)) {
          expect(Number(uLong)).feature(Number::toString).toEqual(uLong.toString())
        }
        checkAll(PropTestConfig(iterations = 200), Arb.uLong()) { uLong ->
          expect(Number(uLong)).feature(Number::toString).toEqual(uLong.toString())
        }
      }

      it("can be created from floats (except NaN)") {
        for (float in listOf(
          0.0f, -0.0f, 1.0f, -1.0f, Float.MIN_VALUE, -Float.MIN_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE,
          Float.fromBits(2), Float.fromBits(0x007fffff), Float.fromBits(0x00800000), Float.fromBits(0x00800001),
          Float.fromBits(1.0f.toBits() - 1), Float.fromBits(1.0f.toBits() + 1), Float.fromBits(Float.MAX_VALUE.toBits() - 1),
        )) {
          expect { Number(float) }.withRepresentation("Number($float)").notToThrow()
        }
        checkAll(PropTestConfig(iterations = 100), Arb.float(includeNaNs = false)) { float ->
          expect { Number(float) }.withRepresentation("Number($float)").notToThrow()
        }
        expect(Number(2.5f)).toEqualNumerically(Number(5) / Number(2))
      }

      it("can be created from doubles (except NaN)") {
        for (double in doubleBoundaries) {
          expect { Number(double) }.withRepresentation("Number($double)").notToThrow()
        }
        checkAll(PropTestConfig(iterations = 100), Arb.double(includeNaNs = false)) { double ->
          expect { Number(double) }.withRepresentation("Number($double)").notToThrow()
        }
        expect(Number(2.5)).toEqualNumerically(Number(5) / Number(2))
      }
    }

    // Integers are held either as Integer or, e.g. after a division, as Fraction. Each operation is implemented separately
    // for every combination of the two, so all combinations are checked against Long arithmetic on Int operands.
    describe("arithmetic") {
      describe("unaryMinus negates") {
        withIts(Representation.entries) { representation ->
          for (int in intBoundaries) {
            expect(-representation.of(int)).toEqualNumerically(Number(-int.toLong()))
          }
          checkAll(PropTestConfig(iterations = 100), Arb.int()) { int ->
            expect(-representation.of(int)).toEqualNumerically(Number(-int.toLong()))
          }
        }
      }

      describe("plus") {
        withIts(representationPairs) { (left, right) ->
          checkAll(PropTestConfig(iterations = 200), Arb.int(), Arb.int()) { a, b ->
            expect(left.of(a) + right.of(b)).toEqualNumerically(Number(a.toLong() + b))
          }
        }
      }

      describe("minus") {
        withIts(representationPairs) { (left, right) ->
          checkAll(PropTestConfig(iterations = 200), Arb.int(), Arb.int()) { a, b ->
            expect(left.of(a) - right.of(b)).toEqualNumerically(Number(a.toLong() - b))
          }
        }
      }

      describe("times") {
        withIts(representationPairs) { (left, right) ->
          checkAll(PropTestConfig(iterations = 200), Arb.int(), Arb.int()) { a, b ->
            expect(left.of(a) * right.of(b)).toEqualNumerically(Number(a.toLong() * b))
          }
        }
      }

      describe("div") {
        describe("divides") {
          withIts(representationPairs) { (left, right) ->
            checkAll(PropTestConfig(iterations = 200), Arb.int(), Arb.int().filter { it != 0 }) { a, b ->
              expect((left.of(a) / right.of(b)) * Number(b)).toEqualNumerically(Number(a))
            }
          }
        }

        // Zero dividends are included because 0/0 has been mishandled by the JVM backend of exact-numbers.
        describe("rejects division by zero") {
          withIts(representationPairs) { (left, right) ->
            for (dividend in listOf(0, 1)) {
              expect { left.of(dividend) / right.of(0) }.toThrow<ArithmeticException>()
            }
          }
        }
      }
    }

    describe("compareTo") {
      // A small range makes equal operands frequent.
      describe("orders by value") {
        withIts(representationPairs) { (left, right) ->
          checkAll(Exhaustive.ints(-3..3), Exhaustive.ints(-3..3)) { a, b ->
            expect(left.of(a).compareTo(right.of(b)).sign).toEqual(a.compareTo(b).sign)
          }
        }
      }

      it("orders non-integers like the doubles they were created from (excluding -0.0, NaN, -∞, ∞") {
        var eligibleDoubles = Arb.double(includeNaNs = false).filter { it != -0.0 }
        for (left in doubleBoundaries.filter { it != 0.0 }) {
          for (right in doubleBoundaries.filter { it != 0.0 }) {
            expect(Number(left).compareTo(Number(right)).sign).toEqual(left.compareTo(right).sign)
          }
        }
        checkAll(PropTestConfig(iterations = 200), eligibleDoubles, eligibleDoubles) { left, right ->
          expect(Number(left).compareTo(Number(right)).sign).toEqual(left.compareTo(right).sign)
        }
      }
    }

    describe("isInteger") {
      withIts(
        mapOf(
          "recognises an Integer" to (Number(5) to true),
          "recognises an integer-valued Fraction" to (Number(10) / Number(2) to true),
          "rejects a non-integer Fraction" to (Number(1) / Number(2) to false),
        )
      ) { (number, isInteger) ->
        expect(number.isInteger()).toEqual(isInteger)
      }
    }

    // Numbers are values (e.g. map keys), so equality must depend on the value alone, not on the representation.
    describe("equals & hashCode") {
      it("considers an integer and the integer-valued fraction with the same value equal") {
        for (long in longBoundaries) {
          val integer = Number(long)
          val fraction = Number(long) / Number(1)
          expect(integer).toEqual(fraction)
          expect(fraction).toEqual(integer)
          expect(integer.hashCode()).toEqual(fraction.hashCode())
        }
        checkAll(PropTestConfig(iterations = 200), Arb.long()) { long ->
          val integer = Number(long)
          val fraction = Number(long) / Number(1)
          expect(integer) {
            toEqual(fraction)
            feature(Number::hashCode).toEqual(fraction.hashCode())
          }
          expect(fraction) {
            toEqual(integer)
            feature(Number::hashCode).toEqual(integer.hashCode())
          }
        }
      }

      // A small range makes equal operands frequent.
      it("agrees with compareTo, regardless of representation") {
        checkAll(
          Exhaustive.ints(-3..3),
          Exhaustive.ints(-3..3),
          Exhaustive.enum<Representation>(),
          Exhaustive.enum<Representation>()
        ) { a, b, leftRepresentation, rightRepresentation ->
          val left = leftRepresentation.of(a)
          val right = rightRepresentation.of(b)
          expect(left == right).toEqual(left.compareTo(right) == 0)
        }
      }

      describe("considers equal values from different sources equal and gives them the same hash code") {
        withIts(
          mapOf(
            "2 and 2.0" to (Number(2) to Number(2.0)),
            "0 and -0.0" to (Number(0) to Number(-0.0)),
            "1/2 and 0.5" to (Number(1) / Number(2) to Number(0.5)),
          )
        ) { (left, right) ->
          expect(left) {
            toEqual(right)
            feature(Number::hashCode).toEqual(right.hashCode())
          }
          expect(right) {
            toEqual(left)
            feature(Number::hashCode).toEqual(left.hashCode())
          }
        }
      }
    }

    // Integers are printed by the creation tests above.
    describe("toString") {
      it("prints integer-valued fractions like integers") {
        for (long in longBoundaries) {
          expect((Number(long) / Number(1)).toString()).toEqual(long.toString())
        }
        checkAll(PropTestConfig(iterations = 200), Arb.long()) { long ->
          expect((Number(long) / Number(1)).toString()).toEqual(long.toString())
        }
      }

      describe("prints integers created from other sources like integers") {
        withIts(
          mapOf(
            "2.0" to (Number(2.0) to "2"),
            "-0.0" to (Number(-0.0) to "0"),
          )
        ) { (number, expected) ->
          expect(number.toString()).toEqual(expected)
        }
      }

      describe("prints fractions using raised and lowered digits around a fraction slash") {
        withIts(
          mapOf(
            "one half" to (Number(1) / Number(2) to "¹⁄₂"),
            "negative one eighth" to (Number(1) / Number(-8) to "-¹⁄₈"),
            "an improper reducible fraction" to (Number(6) / Number(4) to "1 ¹⁄₂"),
            "an improper fraction" to (Number(16) / Number(3) to "5 ¹⁄₃"),
            "a negative improper fraction" to (Number(-22) / Number(7) to "-3 ¹⁄₇"),
            "a fraction with a multi-digit denominator" to (Number(1) / Number(200) to "¹⁄₂₀₀"),
            "a value beyond the platform integer range" to
              (Number("100000000000000000000000000000000000001") / Number(2) to
                "50000000000000000000000000000000000000 ¹⁄₂"),
            "the exact value of the double 0.1" to
              (Number(0.1) to "³⁶⁰²⁸⁷⁹⁷⁰¹⁸⁹⁶³⁹⁷⁄₃₆₀₂₈₇₉₇₀₁₈₉₆₃₉₆₈"),
          )
        ) { (number, expected) ->
          expect(number.toString()).toEqual(expected)
        }
      }
    }

    describe("parsing") {
      it("round trips the printed form of any number") {
        checkAll(numbers) { number ->
          expect(Number(number.toString())).toEqual(number)
        }
      }

      it("accepts either slash and independently ordinary numerator and denominator digits") {
        fun checkNotation(number: Number) {
          for (slash in listOf('⁄', '/')) {
            for (numeratorDigits in listOf("⁰¹²³⁴⁵⁶⁷⁸⁹", "0123456789")) {
              for (denominatorDigits in listOf("₀₁₂₃₄₅₆₇₈₉", "0123456789")) {
                val input = number.toString().map { char ->
                  when (char) {
                    in "⁰¹²³⁴⁵⁶⁷⁸⁹" -> numeratorDigits["⁰¹²³⁴⁵⁶⁷⁸⁹".indexOf(char)]
                    in "₀₁₂₃₄₅₆₇₈₉" -> denominatorDigits["₀₁₂₃₄₅₆₇₈₉".indexOf(char)]
                    '⁄' -> slash
                    else -> char
                  }
                }.joinToString("")
                expect(Number(input)).toEqual(number)
              }
            }
          }
        }
        for (number in listOf(Number(0), Number(1), Number(-1), Number(Long.MIN_VALUE), Number(Long.MAX_VALUE), Number(1) / Number(2), Number(-7) / Number(3), Number("1267650600228229401496703205376") / Number(3))) {
          checkNotation(number)
        }
        checkAll(PropTestConfig(iterations = 200), numbers) { number -> checkNotation(number) }
      }

      it("resolves the last digit group as whole-part digits or numerator digits when the input ends or a slash arrives") {
        fun checkGroups(first: Int, middle: Int, last: Int, denominator: Int) {
          val prefix = "$first  ${middle.toString().padStart(3, '0')}"
          val groupedWhole = Number(first * 1000 + middle)
          expect(Number("$prefix   ${last.toString().padStart(3, '0')}  "))
            .toEqual(groupedWhole * Number(1000) + Number(last))
          expect(Number("$prefix   $last / $denominator  "))
            .toEqual(groupedWhole + Number(last) / Number(denominator))
        }
        for (digits in listOf(0, 1, 998, 999)) {
          for (denominator in listOf(1, 2, 998, 999)) {
            checkGroups(digits, digits, digits, denominator)
          }
        }
        checkAll(PropTestConfig(iterations = 100), Arb.int(0..999), Arb.int(0..999), Arb.int(0..999), Arb.int(1..999)) { first, middle, last, denominator ->
          checkGroups(first, middle, last, denominator)
        }
      }

      describe("accepts non-canonical fractions") {
        withIts(
          mapOf(
            "unreduced mixed fraction with an integer result" to ("5 20/4" to Number(10)),
            "unreduced mixed fraction with a fractional result" to ("5 21/4" to Number(41) / Number(4)),
            "unreduced raised and lowered fraction" to ("5 ²⁰⁄₄" to Number(10)),
            "unreduced proper fraction" to ("2/4" to Number(1) / Number(2)),
            "improper fraction" to ("20/4" to Number(5)),
            "negative mixed fraction" to ("-5 21/4" to Number(-41) / Number(4)),
            "zero numerator" to ("0/4" to Number(0)),
            "zero mixed numerator" to ("5 0/4" to Number(5)),
            "raised zero numerator" to ("⁰/₄" to Number(0)),
            "zero whole part" to ("0 1/4" to Number(1) / Number(4)),
            "negative zero" to ("-0 0/4" to Number(0)),
            "leading zeros" to ("005 0020/0004" to Number(10)),
            "raised and lowered leading zeros" to ("005 ⁰⁰²⁰/₀₀₀₄" to Number(10)),
            "surrounding spaces" to ("  - 5 20 / 4  " to Number(-10)),
            "spaces around a styled fraction slash" to ("  - 5 ²⁰ ⁄ ₄  " to Number(-10)),
            "multiple spaces between ordinary digit groups" to ("1  000   20 / 4  " to Number(1005)),
            "multiple spaces around a styled mixed fraction" to ("1  000   ²⁰ / ₄  " to Number(1005)),
            "grouped whole part" to ("1 000 20/4" to Number(1005)),
            "grouped whole part before a raised numerator" to ("1 000 ²⁰/₄" to Number(1005)),
            "narrow no-break space before a plain numerator" to ("5 20/4" to Number(10)),
            "thin space before a plain numerator" to ("5 20/4" to Number(10)),
            "the last plain digit group is the numerator" to ("1 000/2" to Number(1)),
            "a denominator beyond the platform integer range" to
              ("1/100000000000000000000000000000000000001" to
                Number(1) / Number("100000000000000000000000000000000000001")),
          )
        ) { (input, expected) ->
          expect(Number(input)).toEqual(expected)
        }
      }

      it("parses integers to the same number as the corresponding long") {
        for (long in longBoundaries) {
          expect(Number(long.toString())).toEqual(Number(long))
        }
        checkAll(PropTestConfig(iterations = 200), Arb.long()) { long ->
          expect(Number(long.toString())).toEqual(Number(long))
        }
      }

      // Parsing and printing could agree on a wrong value, which the test above excludes; this one covers integers
      // beyond the Long range.
      it("prints parsed integers of any length exactly as they were written") {
        expect(Number("0").toString()).toEqual("0")
        checkAll(canonicalIntegerStrings) { integerString ->
          expect(Number(integerString).toString()).toEqual(integerString)
        }
      }

      describe("accepts non-canonical integers") {
        withIts(
          mapOf(
            "leading zeros" to ("007" to 7L),
            "negative zero" to ("-0" to 0L),
            "leading and trailing spaces" to (" 42 " to 42L),
            "space after the minus sign" to ("- 5" to -5L),
            "spaces as digit group separators" to ("1 000 000" to 1_000_000L),
            "no-break space as digit group separator" to ("1 000" to 1_000L),
            "narrow no-break space as digit group separator" to ("1 000" to 1_000L),
            "thin space as digit group separator" to ("1 000" to 1_000L),
          )
        ) { (integerString, expected) ->
          expect(Number(integerString)).toEqual(Number(expected))
        }
      }

      describe("explains invalid input") {
        withIts(
          mapOf(
            "empty" to ("" to "Expected at least one digit."),
            "only spaces" to ("  " to "Expected at least one digit."),
            "only a minus sign" to ("-" to "Expected at least one digit."),
            "a minus sign followed by spaces" to ("- " to "Expected at least one digit."),
            "two minus signs" to ("--5" to "A minus sign is only allowed at the start of the number (position 2)."),
            "a trailing minus sign" to ("5-" to "A minus sign is only allowed at the start of the number (position 2)."),
            "a leading plus sign" to ("+5" to "A plus sign is not supported; omit '+' (position 1)."),
            "letters" to ("abc" to "Unexpected character 'a' at position 1 in the whole part; expected ordinary digits (0–9)."),
            "digits followed by a letter" to
              ("12a" to "Unexpected character 'a' at position 3 in the whole part; expected ordinary digits (0–9)."),
            "non-ASCII digits" to ("١٢" to "Unexpected character '١' at position 1 in the whole part; expected ordinary digits (0–9)."),
            "fullwidth digits" to ("５" to "Unexpected character '５' at position 1 in the whole part; expected ordinary digits (0–9)."),
            "a decimal point" to ("1.5" to "Decimal notation is not supported; write a fraction such as '1/2' instead (position 2)."),
            "a decimal comma" to ("1,5" to "Decimal notation is not supported; write a fraction such as '1/2' instead (position 2)."),
            "a missing numerator" to ("/2" to "Missing numerator before '/' at position 1."),
            "a missing raised numerator" to ("⁄₂" to "Missing numerator before '⁄' at position 1."),
            "a missing numerator after a sign" to (" - /2" to "Missing numerator before '/' at position 4."),
            "a missing denominator" to ("1/" to "Missing denominator after '/' at position 2."),
            "a missing lowered denominator" to ("¹⁄" to "Missing denominator after '⁄' at position 2."),
            "a denominator containing only spaces" to ("1/  " to "Missing denominator after '/' at position 2."),
            "a separator without either part" to ("/" to "Missing numerator before '/' at position 1."),
            "a zero denominator" to ("1/0" to "The denominator must not be zero (position 3)."),
            "zero over zero" to ("0/0" to "The denominator must not be zero (position 3)."),
            "a lowered zero denominator" to ("¹⁄₀" to "The denominator must not be zero (position 3)."),
            "a zero denominator with leading zeros" to ("5 20/000" to "The denominator must not be zero (position 6)."),
            "a negative numerator" to ("5 -1/2" to "A minus sign is only allowed at the start of the number (position 3)."),
            "a negative denominator" to ("1/-2" to "A minus sign is only allowed at the start of the number (position 3)."),
            "a raised minus sign" to ("⁻¹/₂" to "A minus sign is only allowed at the start of the number; use ordinary '-' (position 1)."),
            "a lowered minus sign" to ("¹/₋₂" to "A minus sign is only allowed at the start of the number; use ordinary '-' (position 3)."),
            "two fractions" to
              ("1/2 3/4" to "The fraction must be at the end; unexpected '3' at position 5 after the denominator."),
            "another separator after denominator spaces" to ("1/2  /3" to "Only one fraction is allowed; found another '/' at position 6."),
            "chained division" to ("1/2/3" to "Only one fraction is allowed; found another '/' at position 4."),
            "two different separators" to ("1⁄2/3" to "Only one fraction is allowed; found another '/' at position 4."),
            "repeated separators" to ("1//2" to "Only one fraction is allowed; found another '/' at position 3."),
            "a whole part after a fraction" to
              ("1/2 3" to "The fraction must be at the end; unexpected '3' at position 5 after the denominator."),
            "lowered digits after a fraction" to
              ("¹⁄₂ ₃" to "The fraction must be at the end; unexpected '₃' at position 5 after the denominator."),
            "a letter in the numerator" to
              ("1a/2" to "Unexpected character 'a' at position 2 in the numerator; expected ordinary or raised digits."),
            "a letter in the denominator" to
              ("1/2a" to "Unexpected character 'a' at position 4 in the denominator; expected ordinary or lowered digits."),
            "a letter in the mixed whole part" to
              ("5x 1/2" to "Unexpected character 'x' at position 2 in the whole part; expected ordinary digits (0–9)."),
            "raised digits without a fraction" to ("¹" to "Raised digit '¹' at position 1 is only allowed in a fraction's numerator."),
            "lowered digits without a fraction" to ("₁" to "Lowered digit '₁' at position 1 is only allowed in a fraction's denominator."),
            "a raised whole part" to ("⁵ 1/2" to "Raised digit '⁵' at position 1 is only allowed in a fraction's numerator."),
            "a lowered whole part" to ("₅ 1/2" to "Lowered digit '₅' at position 1 is only allowed in a fraction's denominator."),
            "a lowered numerator" to ("₁/2" to "Lowered digit '₁' at position 1 is only allowed in a fraction's denominator."),
            "a raised denominator" to ("1/²" to "Raised digit '²' at position 3 is only allowed in a fraction's numerator."),
            "a partly lowered numerator" to ("1₂/3" to "Lowered digit '₂' at position 2 is only allowed in a fraction's denominator."),
            "a partly raised denominator" to ("1/2³" to "Raised digit '³' at position 4 is only allowed in a fraction's numerator."),
            "ordinary then raised numerator digits" to
              ("1²/3" to "The numerator must use ordinary or raised digits consistently; found '²' at position 2."),
            "raised then ordinary numerator digits" to
              ("¹2/3" to "The numerator must use ordinary or raised digits consistently; found '2' at position 2."),
            "a mixed-style mixed numerator" to
              ("5 2⁰/4" to "The numerator must use ordinary or raised digits consistently; found '⁰' at position 4."),
            "ordinary then lowered denominator digits" to
              ("1/2₃" to "The denominator must use ordinary or lowered digits consistently; found '₃' at position 4."),
            "lowered then ordinary denominator digits" to
              ("1/₂3" to "The denominator must use ordinary or lowered digits consistently; found '3' at position 4."),
          )
        ) { (input, message) ->
          expect { Number(input) }.toThrow<IllegalArgumentException>()
            .message.toEqual("Invalid number \"$input\": $message")
        }
      }

      it("makes invisible characters visible in errors and counts positions in the original input") {
        expect { Number(" \t1/2") }.toThrow<IllegalArgumentException>()
          .message.toEqual(
            "Invalid number \" \\t1/2\": Unexpected character '\\t' at position 2 in the numerator; expected ordinary or raised digits.",
          )
        expect { Number("1/2\n") }.toThrow<IllegalArgumentException>()
          .message.toEqual(
            "Invalid number \"1/2\\n\": Unexpected character '\\n' at position 4 in the denominator; expected ordinary or lowered digits.",
          )
        expect { Number("1\r2") }.toThrow<IllegalArgumentException>()
          .message.toEqual(
            "Invalid number \"1\\r2\": Unexpected character '\\r' at position 2 in the whole part; expected ordinary digits (0–9).",
          )
        expect { Number("1\u200b2") }.toThrow<IllegalArgumentException>()
          .message.toEqual(
            """Invalid number "1\u200b2": Unexpected character '\u200b' at position 2 in the whole part; expected ordinary digits (0–9).""",
          )
        expect { Number("1\u00002") }.toThrow<IllegalArgumentException>()
          .message.toEqual(
            """Invalid number "1\u00002": Unexpected character '\u0000' at position 2 in the whole part; expected ordinary digits (0–9).""",
          )
      }
    }
    describe("arithmetic at explicit Int boundaries") {
      withIts(representationPairs) { (left, right) ->
        for (a in intBoundaries) {
          for (b in intBoundaries) {
            expect(left.of(a) + right.of(b)).toEqualNumerically(Number(a.toLong() + b))
            expect(left.of(a) - right.of(b)).toEqualNumerically(Number(a.toLong() - b))
            expect(left.of(a) * right.of(b)).toEqualNumerically(Number(a.toLong() * b))
            if (b != 0) {
              expect((left.of(a) / right.of(b)) * Number(b)).toEqualNumerically(Number(a))
            }
          }
        }
      }
    }
  }
})

private val intBoundaries = listOf(Int.MIN_VALUE, Int.MIN_VALUE + 1, -1, 0, 1, Int.MAX_VALUE - 1, Int.MAX_VALUE)
private val longBoundaries = listOf(
  Long.MIN_VALUE, Long.MIN_VALUE + 1, -1L, 0L, 1L, Long.MAX_VALUE - 1, Long.MAX_VALUE,
  (1L shl 53) - 1, 1L shl 53, (1L shl 53) + 1,
  -(1L shl 53) - 1, -(1L shl 53), -(1L shl 53) + 1,
)
private val doubleBoundaries = listOf(
  0.0, -0.0, 1.0, -1.0, Double.MIN_VALUE, -Double.MIN_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE,
  Double.fromBits(2), Double.fromBits(0x000fffffffffffff), Double.fromBits(0x0010000000000000), Double.fromBits(0x0010000000000001),
  Double.fromBits(1.0.toBits() - 1), Double.fromBits(1.0.toBits() + 1), Double.fromBits(Double.MAX_VALUE.toBits() - 1),
)

private enum class Representation {
  Integer {
    override fun of(int: Int) = Number(int)
  },
  IntegerValuedFraction {
    override fun of(int: Int) = Number(int) / Number(1)
  };

  abstract fun of(int: Int): Number
}

private data class RepresentationPair(val left: Representation, val right: Representation) {
  override fun toString() = "$left with $right"
}

private val representationPairs =
  Representation.entries.flatMap { left -> Representation.entries.map { right -> RepresentationPair(left, right) } }

private val canonicalIntegerStrings =
  Arb.bind(Arb.boolean(), Arb.int(1..9), Arb.list(Arb.int(0..9), 0..60)) { isNegative, firstDigit, otherDigits ->
    (if (isNegative) "-" else "") + firstDigit + otherDigits.joinToString("")
  }

private val numbers = Arb.choice(
  Arb.long().map { Number(it) },
  canonicalIntegerStrings.map { Number(it) },
  Arb.float(includeNaNs = false).map { Number(it) },
  Arb.double(includeNaNs = false).map { Number(it) },
  Arb.bind(canonicalIntegerStrings, canonicalIntegerStrings) { numerator, denominator ->
    Number(numerator) / Number(denominator)
  },
)

/** Asserts equality by [compareTo], so arithmetic tests do not depend on [equals] (which is tested separately). */
private fun Expect<Number>.toEqualNumerically(expected: Number) =
  _logic.createAndAppend("to equal numerically", expected) { it.compareTo(expected) == 0 }
