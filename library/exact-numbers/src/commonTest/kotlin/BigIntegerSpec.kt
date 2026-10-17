package software.consistent.exactnumbers

import ch.tutteli.atrium.api.fluent.en_GB.*
import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.bytes
import io.kotest.property.exhaustive.ints
import io.kotest.property.exhaustive.map
import kotlin.math.sign

class BigIntegerSpec : DescribeSpec({
  describe("BigInteger") {
    // Each primitive type has its own conversion on each target. The extremes are checked explicitly because random
    // generation does not guarantee to hit them.
    describe("toBigInteger – conversion from primitives") {
      it("converts bytes") { checkConversion(Exhaustive.bytes()) { it.toBigInteger() } }
      it("converts uBytes") { checkConversion(Exhaustive.ints(0..255).map { it.toUByte() }) { it.toBigInteger() } }
      it("converts shorts") { checkConversion(Arb.short(), Short.MIN_VALUE, Short.MAX_VALUE) { it.toBigInteger() } }
      it("converts uShorts") { checkConversion(Arb.uShort(), UShort.MIN_VALUE, UShort.MAX_VALUE) { it.toBigInteger() } }
      it("converts ints") { checkConversion(Arb.int(), Int.MIN_VALUE, Int.MAX_VALUE) { it.toBigInteger() } }
      it("converts uInts") { checkConversion(Arb.uInt(), UInt.MIN_VALUE, UInt.MAX_VALUE) { it.toBigInteger() } }
      it("converts longs") { checkConversion(Arb.long(), Long.MIN_VALUE, Long.MAX_VALUE) { it.toBigInteger() } }
      it("converts uLongs") { checkConversion(Arb.uLong(), ULong.MIN_VALUE, ULong.MAX_VALUE) { it.toBigInteger() } }
    }

    describe("arithmetic") {
      describe("unaryMinus") {
        it("negates") {
          checkAll(Arb.long(min = Long.MIN_VALUE + 1)) { long ->
            expect(-(long.toBigInteger())).toEqual((-long).toBigInteger())
          }
        }

        it("negates Long.MIN_VALUE, whose negation does not fit into a Long") {
          expect(-Long.MIN_VALUE.toBigInteger()).toEqual(twoToThe63)
        }
      }

      describe("absoluteValue") {
        it("computes the absolute value") {
          checkAll(Arb.long(min = Long.MIN_VALUE + 1)) { long ->
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
          checkAll(Arb.int(), Arb.int()) { left, right ->
            expect(left.toBigInteger() + right.toBigInteger()).toEqual((left.toLong() + right).toBigInteger())
          }
        }

        it("adds beyond the Long range") {
          expect(Long.MAX_VALUE.toBigInteger() + BigIntegers.ONE).toEqual(twoToThe63)
        }
      }

      describe("minus") {
        it("subtracts") {
          checkAll(Arb.int(), Arb.int()) { left, right ->
            expect(left.toBigInteger() - right.toBigInteger()).toEqual((left.toLong() - right).toBigInteger())
          }
        }

        it("subtracts beyond the Long range") {
          expect(Long.MIN_VALUE.toBigInteger() - BigIntegers.ONE).toEqual(-(twoToThe63 + BigIntegers.ONE))
        }
      }

      describe("times") {
        it("multiplies") {
          checkAll(Arb.int(), Arb.int()) { left, right ->
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
          checkAll(Arb.int(), Arb.int().filter { it != 0 }) { dividend, divisor ->
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
          withData(listOf(
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
          checkAll(Arb.bigInteger()) { integer ->
            expect(integer shl 0).toEqual(integer)
          }
        }

        it("multiplies by powers of two") {
          checkAll(Arb.bigInteger(), Arb.int(0, 50)) { integer, bits ->
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
          checkAll(Arb.bigInteger(), Arb.int(0, 50)) { integer, bits ->
            expect((-integer) shl bits).toEqual(-(integer shl bits))
          }
        }

        it("chains shifts additively") {
          checkAll(Arb.bigInteger(), Arb.int(0, 50), Arb.int(0, 50)) { integer, a, b ->
            expect((integer shl a) shl b).toEqual(integer shl (a + b))
          }
        }

        it("shifts right when given a negative bit count") {
          expect(8.toBigInteger() shl -2).toEqual(2.toBigInteger())
          expect(twoToThe(64) shl -64).toEqual(BigIntegers.ONE)
          checkAll(Arb.bigInteger(), Arb.int(0, 100)) { integer, bits ->
            expect(integer shl -bits).toEqual(integer shr bits)
          }
        }
        it("is reversed by shr for any integer") {
          checkAll(Arb.bigInteger(), Arb.int(0, 100)) { integer, bits ->
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
          checkAll(Arb.bigInteger()) { integer ->
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
          checkAll(Arb.bigInteger(), Arb.int(0, 50), Arb.int(0, 50)) { integer, a, b ->
            expect((integer shr a) shr b).toEqual(integer shr (a + b))
          }
        }

        it("shifts left when given a negative bit count") {
          expect(BigIntegers.TWO shr -2).toEqual(8.toBigInteger())
          expect(BigIntegers.ONE shr -64).toEqual(twoToThe(64))
          checkAll(Arb.bigInteger(), Arb.int(0, 100)) { integer, bits ->
            expect(integer shr -bits).toEqual(integer shl bits)
          }
        }
      }
    }


    describe("compareTo") {
      it("orders like Long") {
        checkAll(Arb.long(), Arb.long()) { left, right ->
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
        checkAll(Arb.bigInteger(), Arb.bigInteger()) { integer, other ->
          val recomputed = integer + other - other
          expect(recomputed) {
            toEqual(integer)
            feature(BigInteger::hashCode).toEqual(integer.hashCode())
          }
        }
      }
    }
  }
})

private suspend fun <T> checkConversion(gen: Gen<T>, vararg extremes: T, convert: (T) -> BigInteger) {
  for (extreme in extremes) {
    expect(convert(extreme).toString()).toEqual(extreme.toString())
  }
  checkAll(gen) { value ->
    expect(convert(value).toString()).toEqual(value.toString())
  }
}
