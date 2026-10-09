package software.consistent.model.type

import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withIts
import io.kotest.property.Arb
import io.kotest.property.checkAll
import software.consistent.model.generators.value
import software.consistent.model.type.solvers.ValuePartition
import software.consistent.model.type.solvers.ValuePartition.Symbol

class AnySymbolSpec : DescribeSpec({
  describe(AnySymbol::class.simpleName!!) {
    it("contains any symbol") {
      checkAll(Arb.value.symbol()) { symbol ->
        expect(AnySymbol()).toContain(symbol)
      }
    }

    withIts(
      nameFn = { "contains no ${it.name}" },
      ValuePartition.entries.filter { it != Symbol }) { valuePartition ->
      checkAll(Arb.value(valuePartition)) { value ->
        expect(AnySymbol()).notToContain(value)
      }
    }
  }
})

