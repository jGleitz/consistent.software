package software.consistent.data.type

import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withIts
import software.consistent.data.value.FALSE
import software.consistent.data.value.Map
import software.consistent.data.value.Number
import software.consistent.data.value.Symbol
import software.consistent.data.value.TRUE

class AnyNumberSpec : DescribeSpec({
  describe(AnyNumber::class.simpleName!!) {
    describe("contains any number") {
      withIts(listOf(
        Number(0), Number(1), Number(-1), Number(Long.MIN_VALUE), Number(Long.MAX_VALUE),
        Number("1267650600228229401496703205376"),
        Number(1) / Number(2), Number(-1) / Number(2), Number(7) / Number(3), Number(-7) / Number(3),
      )) { number ->
        expect(AnyNumber()).toAllow(number)
      }
    }

    describe("contains no Symbol") {
      withIts(mapOf("TRUE" to TRUE, "FALSE" to FALSE, "ordinary" to Symbol("ordinary"), "empty" to Symbol(""))) { symbol ->
        expect(AnyNumber()).toForbid(symbol)
      }
    }

    describe("contains no Map") {
      withIts(mapOf(
        "empty" to Map(),
        "non-empty" to Map(Symbol("key") to Number(1)),
        "nested" to Map(Symbol("nested") to Map(Symbol("key") to TRUE)),
      )) { map ->
        expect(AnyNumber()).toForbid(map)
      }
    }
  }
})

