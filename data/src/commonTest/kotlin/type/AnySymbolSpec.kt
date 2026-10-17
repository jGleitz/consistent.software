package software.consistent.data.type

import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec
import software.consistent.data.value.FALSE
import software.consistent.data.value.Map
import software.consistent.data.value.Number
import software.consistent.data.value.Symbol
import software.consistent.data.value.TRUE

class AnySymbolSpec : DescribeSpec({
  describe(AnySymbol::class.simpleName!!) {
    it("contains any symbol") {
      for (symbol in listOf(TRUE, FALSE, Symbol("ordinary"), Symbol(""))) {
        expect(AnySymbol()).toAllow(symbol)
      }
    }

    it("contains no Number") {
      for (number in listOf(
        Number(0), Number(1), Number(-1), Number(Long.MIN_VALUE), Number(Long.MAX_VALUE),
        Number("1267650600228229401496703205376"),
        Number(1) / Number(2), Number(-1) / Number(2), Number(7) / Number(3), Number(-7) / Number(3),
      )) {
        expect(AnySymbol()).toForbid(number)
      }
    }

    it("contains no Map") {
      for (map in listOf(Map(), Map(Symbol("key") to Number(1)), Map(Symbol("nested") to Map(Symbol("key") to TRUE)))) {
        expect(AnySymbol()).toForbid(map)
      }
    }
  }
})

