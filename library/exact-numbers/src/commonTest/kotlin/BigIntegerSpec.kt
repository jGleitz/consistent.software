package software.consistent.exactnumbers

import ch.tutteli.atrium.api.fluent.en_GB.*
import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withIts
import io.kotest.property.*
import io.kotest.property.arbitrary.*
import io.kotest.property.exhaustive.bytes
import io.kotest.property.exhaustive.ints
import kotlin.math.sign

class BigIntegerSpec : DescribeSpec({
  describe("BigInteger") {
    // Each primitive type has its own conversion on each target. The extremes are checked explicitly because random
    // generation does not guarantee to hit them.
    describe("toBigInteger – conversion from primitives") {
      it("converts bytes") { checkConversion(Exhaustive.bytes()) { it.toBigInteger() } }
      it("converts uBytes") { checkConversion(Exhaustive.uBytes()) { it.toBigInteger() } }
      it("converts shorts") {
        checkConversion(
          Arb.short(), Short.MIN_VALUE, (Short.MIN_VALUE + 1).toShort(), (-1).toShort(), 0.toShort(), 1.toShort(),
          (Short.MAX_VALUE - 1).toShort(), Short.MAX_VALUE, iterations = 100,
        ) { it.toBigInteger() }
      }
      it("converts uShorts") {
        checkConversion(
          Arb.uShort(), UShort.MIN_VALUE, 1u.toUShort(), 32767u.toUShort(), 32768u.toUShort(), 32769u.toUShort(),
          (UShort.MAX_VALUE - 1u).toUShort(), UShort.MAX_VALUE, iterations = 100,
        ) { it.toBigInteger() }
      }
      it("converts ints") {
        checkConversion(Arb.int(), *intBoundaries.toTypedArray(), iterations = 200) { it.toBigInteger() }
      }
      it("converts uInts") {
        checkConversion(
          Arb.uInt(), UInt.MIN_VALUE, 1u, Int.MAX_VALUE.toUInt(), Int.MAX_VALUE.toUInt() + 1u, Int.MAX_VALUE.toUInt() + 2u,
          UInt.MAX_VALUE - 1u, UInt.MAX_VALUE, iterations = 200,
        ) { it.toBigInteger() }
      }
      it("converts longs") {
        checkConversion(Arb.long(), *longBoundaries.toTypedArray(), iterations = 200) { it.toBigInteger() }
      }
      it("converts uLongs") {
        checkConversion(
          Arb.uLong(), ULong.MIN_VALUE, 1uL, Long.MAX_VALUE.toULong(), Long.MAX_VALUE.toULong() + 1uL, Long.MAX_VALUE.toULong() + 2uL,
          ULong.MAX_VALUE - 1uL, ULong.MAX_VALUE,
          *listOf(8, 16, 24, 32, 40, 48, 56).flatMap { bits ->
            val boundary = 1uL shl bits
            listOf(boundary - 1uL, boundary, boundary + 1uL)
          }.toTypedArray(),
          iterations = 200,
        ) { it.toBigInteger() }
      }
    }

    describe("arithmetic") {
      describe("unaryMinus") {
        it("negates") {
          checkAll(iterations = 100, Arb.long(min = Long.MIN_VALUE + 1)) { long ->
            expect(-(long.toBigInteger())).toEqual((-long).toBigInteger())
          }
        }

        it("negates Long.MIN_VALUE, whose negation does not fit into a Long") {
          expect(-Long.MIN_VALUE.toBigInteger()).toEqual(twoToThe63)
        }
      }

      describe("absoluteValue") {
        it("computes the absolute value") {
          checkAll(iterations = 100, Arb.long(min = Long.MIN_VALUE + 1)) { long ->
            expect(long.toBigInteger().absoluteValue).toEqual(kotlin.math.abs(long).toBigInteger())
          }
        }

        it("computes the absolute value of Long.MIN_VALUE, which does not fit into a Long") {
          expect(Long.MIN_VALUE.toBigInteger().absoluteValue).toEqual(twoToThe63)
        }
      }

      describe("plus") {
        // The sums, differences, and products of ints always fit into a long, which gives an exact oracle.
        it("adds") {
          checkAll(iterations = 200, Arb.int(), Arb.int()) { left, right ->
            expect(left.toBigInteger() + right.toBigInteger()).toEqual((left.toLong() + right).toBigInteger())
          }
        }

        it("adds beyond the Long range") {
          expect(Long.MAX_VALUE.toBigInteger() + BigIntegers.ONE).toEqual(twoToThe63)
        }
      }

      describe("minus") {
        it("subtracts") {
          checkAll(iterations = 200, Arb.int(), Arb.int()) { left, right ->
            expect(left.toBigInteger() - right.toBigInteger()).toEqual((left.toLong() - right).toBigInteger())
          }
        }

        it("subtracts beyond the Long range") {
          expect(Long.MIN_VALUE.toBigInteger() - BigIntegers.ONE).toEqual(-(twoToThe63 + BigIntegers.ONE))
        }
      }

      describe("times") {
        it("multiplies") {
          checkAll(iterations = 200, Arb.int(), Arb.int()) { left, right ->
            expect(left.toBigInteger() * right.toBigInteger()).toEqual((left.toLong() * right).toBigInteger())
          }
        }

        it("multiplies beyond the ULong range") {
          val twoToThe32 = (1uL shl 32).toBigInteger()
          expect(twoToThe32 * twoToThe32).toEqual(ULong.MAX_VALUE.toBigInteger() + BigIntegers.ONE)
        }
      }

      describe("divideAndRemainder") {
        it("divides with remainder") {
          checkAll(iterations = 200, Arb.int(), Arb.int().filter { it != 0 }) { dividend, divisor ->
            val (quotient, remainder) = dividend.toBigInteger().divideAndRemainder(divisor.toBigInteger()) { quotient, remainder ->
              quotient to remainder
            }
            expect(quotient).toEqual((dividend.toLong() / divisor.toLong()).toBigInteger())
            expect(remainder).toEqual((dividend.toLong() % divisor.toLong()).toBigInteger())
          }
        }

        it("divides with remainder beyond the Long range") {
          val dividend = twoToThe(100) + 5.toBigInteger()
          dividend.divideAndRemainder(twoToThe(64)) { quotient, remainder ->
            expect(quotient).toEqual(twoToThe(36))
            expect(remainder).toEqual(5.toBigInteger())
          }
        }

        describe("rejects division by zero") {
          withIts(listOf(
            BigIntegers.ZERO, BigIntegers.ONE, -BigIntegers.ONE, 42.toBigInteger(), (-42).toBigInteger(),
            twoToThe(100), -twoToThe(100),
          )) { dividend ->
            expect {
              dividend.divideAndRemainder(BigIntegers.ZERO) { quotient, remainder -> quotient + remainder }
            }.toThrow<ArithmeticException>().and.message.toEqual("Division by zero: attempted to divide $dividend by 0.")
          }
        }
      }
    }

    describe("bit shifts") {
      describe("shl") {
        it("shifts zero to zero") {
          checkAll(Exhaustive.ints(0..100)) { bits ->
            expect(BigIntegers.ZERO shl bits).toEqual(BigIntegers.ZERO)
          }
        }

        it("shifts by zero without changing the value") {
          checkAll(iterations = 100, Arb.bigInteger()) { integer ->
            expect(integer shl 0).toEqual(integer)
          }
        }

        it("multiplies by powers of two") {
          checkAll(iterations = 200, Arb.bigInteger(), Arb.int(0, 50)) { integer, bits ->
            expect(integer shl bits).toEqual(integer * twoToThe(bits))
          }
        }

        it("shifts across 32-bit and 64-bit boundaries") {
          expect(BigIntegers.ONE shl 31).toEqual(0x8000_0000L.toBigInteger())
          expect(BigIntegers.ONE shl 32).toEqual(0x1_0000_0000L.toBigInteger())
          expect(BigIntegers.ONE shl 63).toEqual(twoToThe63)
          expect(BigIntegers.ONE shl 64).toEqual(twoToThe(64))
          expect(Long.MAX_VALUE.toBigInteger() shl 1).toEqual(twoToThe(64) - 2.toBigInteger())
        }

        it("shifts negative numbers preserving sign and magnitude") {
          expect((-1).toBigInteger() shl 1).toEqual((-2).toBigInteger())
          expect((-1).toBigInteger() shl 63).toEqual(-twoToThe63)
          expect((-1).toBigInteger() shl 64).toEqual(-twoToThe(64))
          expect(Long.MIN_VALUE.toBigInteger() shl 1).toEqual(-twoToThe(64))
          checkAll(iterations = 200, Arb.bigInteger(), Arb.int(0, 50)) { integer, bits ->
            expect((-integer) shl bits).toEqual(-(integer shl bits))
          }
        }

        it("chains shifts additively") {
          checkAll(iterations = 200, Arb.bigInteger(), Arb.int(0, 50), Arb.int(0, 50)) { integer, a, b ->
            expect((integer shl a) shl b).toEqual(integer shl (a + b))
          }
        }

        it("shifts right when given a negative bit count") {
          expect(8.toBigInteger() shl -2).toEqual(2.toBigInteger())
          expect(twoToThe(64) shl -64).toEqual(BigIntegers.ONE)
          checkAll(iterations = 100, Arb.bigInteger(), Arb.int(0, 100)) { integer, bits ->
            expect(integer shl -bits).toEqual(integer shr bits)
          }
        }
        it("is reversed by shr for any integer") {
          checkAll(iterations = 200, Arb.bigInteger(), Arb.int(0, 100)) { integer, bits ->
            expect((integer shl bits) shr bits).toEqual(integer)
          }
        }
      }

      describe("shr") {
        it("shifts zero to zero") {
          checkAll(Exhaustive.ints(0..100)) { bits ->
            expect(BigIntegers.ZERO shr bits).toEqual(BigIntegers.ZERO)
          }
        }

        it("shifts by zero without changing the value") {
          checkAll(iterations = 100, Arb.bigInteger()) { integer ->
            expect(integer shr 0).toEqual(integer)
          }
        }

        it("matches Long.shr for positive and negative values") {
          checkAll(Arb.long(), Arb.int(0, 63)) { long, bits ->
            expect(long.toBigInteger() shr bits).toEqual((long shr bits).toBigInteger())
          }
        }

        it("shifts large positive numbers") {
          expect(twoToThe(100) shr 32).toEqual(twoToThe(68))
          expect(twoToThe(100) shr 100).toEqual(BigIntegers.ONE)
          expect(twoToThe(100) shr 101).toEqual(BigIntegers.ZERO)
        }

        // Arithmetic right shift floors negative values towards negative infinity (sign extension).
        it("shifts negative numbers with arithmetic sign extension") {
          expect((-1).toBigInteger() shr 1).toEqual((-1).toBigInteger())
          expect((-1).toBigInteger() shr 100).toEqual((-1).toBigInteger())
          expect((-2).toBigInteger() shr 1).toEqual((-1).toBigInteger())
          expect((-3).toBigInteger() shr 1).toEqual((-2).toBigInteger())
          expect((-4).toBigInteger() shr 1).toEqual((-2).toBigInteger())
          expect((-5).toBigInteger() shr 2).toEqual((-2).toBigInteger())
          expect((-twoToThe(64)) shr 64).toEqual((-1).toBigInteger())
          expect((-twoToThe(64)) shr 63).toEqual((-2).toBigInteger())
        }

        it("chains shifts additively") {
          checkAll(iterations = 200, Arb.bigInteger(), Arb.int(0, 50), Arb.int(0, 50)) { integer, a, b ->
            expect((integer shr a) shr b).toEqual(integer shr (a + b))
          }
        }

        it("shifts left when given a negative bit count") {
          expect(BigIntegers.TWO shr -2).toEqual(8.toBigInteger())
          expect(BigIntegers.ONE shr -64).toEqual(twoToThe(64))
          checkAll(iterations = 100, Arb.bigInteger(), Arb.int(0, 100)) { integer, bits ->
            expect(integer shr -bits).toEqual(integer shl bits)
          }
        }
      }
    }


    describe("compareTo") {
      it("orders like Long") {
        checkAll(iterations = 200, Arb.long(), Arb.long()) { left, right ->
          expect(left.toBigInteger().compareTo(right.toBigInteger()).sign).toEqual(left.compareTo(right).sign)
        }
      }

      // Guards against comparisons that lose precision for large magnitudes, e.g. by going through floating point.
      it("distinguishes neighbouring integers of any magnitude") {
        checkAll(Arb.bigInteger()) { integer ->
          val successor = integer + BigIntegers.ONE
          expect(integer.compareTo(successor).sign).toEqual(-1)
          expect(successor.compareTo(integer).sign).toEqual(1)
          expect(integer.compareTo(integer)).toEqual(0)
        }
      }
    }

    // Values used as map keys must have value-based equals and hashCode, also when computed along different paths.
    describe("equals & hashCode") {
      it("considers equal values computed in different ways equal") {
        checkAll(iterations = 200, Arb.bigInteger(), Arb.bigInteger()) { integer, other ->
          val recomputed = integer + other - other
          expect(recomputed) {
            toEqual(integer)
            feature(BigInteger::hashCode).toEqual(integer.hashCode())
          }
        }
      }
    }
    describe("explicit boundaries for the sampled properties") {
      describe("checks arithmetic at Int limits and their neighbours") {
        withIts(intBoundaries) { left ->
          for (right in intBoundaries) {
            val a = left.toBigInteger()
            val b = right.toBigInteger()
            expect(a + b).toEqual((left.toLong() + right).toBigInteger())
            expect(a - b).toEqual((left.toLong() - right).toBigInteger())
            expect(a * b).toEqual((left.toLong() * right).toBigInteger())
            if (right != 0) {
              a.divideAndRemainder(b) { quotient, remainder ->
                expect(quotient).toEqual((left.toLong() / right).toBigInteger())
                expect(remainder).toEqual((left.toLong() % right).toBigInteger())
              }
            }
          }
        }
      }

      describe("checks negation, absolute value and ordering at Long boundaries") {
        withIts(longBoundaries) { long ->
          if (long != Long.MIN_VALUE) {
            expect(-long.toBigInteger()).toEqual((-long).toBigInteger())
            expect(long.toBigInteger().absoluteValue).toEqual(kotlin.math.abs(long).toBigInteger())
          }
          for (other in longBoundaries) {
            expect(long.toBigInteger().compareTo(other.toBigInteger()).sign).toEqual(long.compareTo(other).sign)
          }
        }
      }

      describe("checks shifts at zero, word boundaries and the sampled range endpoints") {
        withIts(bigIntegerBoundaries) { integer ->
          expect(integer shl 0).toEqual(integer)
          expect(integer shr 0).toEqual(integer)
          for (bits in listOf(0, 1, 31, 32, 33, 49, 50, 63, 64, 65, 99, 100)) {
            expect(integer shl bits).toEqual(integer * twoToThe(bits))
            expect((-integer) shl bits).toEqual(-(integer shl bits))
            expect(integer shl -bits).toEqual(integer shr bits)
            expect(integer shr -bits).toEqual(integer shl bits)
            expect((integer shl bits) shr bits).toEqual(integer)
          }
          for (a in listOf(0, 1, 31, 32, 49, 50)) {
            for (b in listOf(0, 1, 31, 32, 49, 50)) {
              expect((integer shl a) shl b).toEqual(integer shl (a + b))
              expect((integer shr a) shr b).toEqual(integer shr (a + b))
            }
          }
        }
      }

      describe("checks recomputed equality and hash codes at zero, signs and large magnitudes") {
        withIts(bigIntegerBoundaries) { integer ->
          for (other in bigIntegerBoundaries) {
            val recomputed = integer + other - other
            expect(recomputed).toEqual(integer)
            expect(recomputed.hashCode()).toEqual(integer.hashCode())
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
private val bigIntegerBoundaries =
  longBoundaries.map { it.toBigInteger() } + listOf(twoToThe(100) - BigIntegers.ONE, twoToThe(100), -twoToThe(100))

private suspend fun <T> checkConversion(
  gen: Gen<T>,
  vararg extremes: T,
  iterations: Int? = null,
  convert: (T) -> BigInteger,
) {
  for (extreme in extremes) {
    expect(convert(extreme).toString()).toEqual(extreme.toString())
  }
  val property: suspend PropertyContext.(T) -> Unit = { value ->
    expect(convert(value).toString()).toEqual(value.toString())
  }
  if (iterations == null) checkAll(gen, property) else checkAll(iterations = iterations, gen, property)
}
