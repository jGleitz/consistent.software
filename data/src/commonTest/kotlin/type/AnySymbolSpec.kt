package software.consistent.data.type

import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import software.consistent.data.value.FALSE
import software.consistent.data.value.Map
import software.consistent.data.value.Number
import software.consistent.data.value.Symbol
import software.consistent.data.value.TRUE

class AnySymbolSpec : DescribeSpec({
  describe(AnySymbol::class.simpleName!!) {
    describe("contains any symbol") {
      withData(mapOf("TRUE" to TRUE, "FALSE" to FALSE, "ordinary" to Symbol("ordinary"), "empty" to Symbol(""))) { symbol ->
        expect(AnySymbol()).toAllow(symbol)
      }
    }

    describe("contains no Number") {
      withData(listOf(
        Number(0), Number(1), Number(-1), Number(Long.MIN_VALUE), Number(Long.MAX_VALUE),
        Number("1267650600228229401496703205376"),
        Number(1) / Number(2), Number(-1) / Number(2), Number(7) / Number(3), Number(-7) / Number(3),
      )) { number ->
        expect(AnySymbol()).toForbid(number)
      }
    }

    describe("contains no Map") {
      withData(mapOf(
        "empty" to Map(),
        "non-empty" to Map(Symbol("key") to Number(1)),
        "nested" to Map(Symbol("nested") to Map(Symbol("key") to TRUE)),
      )) { map ->
        expect(AnySymbol()).toForbid(map)
      }
    }
  }
})

