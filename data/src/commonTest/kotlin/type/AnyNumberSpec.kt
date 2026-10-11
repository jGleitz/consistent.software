package software.consistent.data.type

import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withIts
import io.kotest.property.Arb
import io.kotest.property.checkAll
import software.consistent.data.generators.value
import software.consistent.data.type.solvers.ValuePartition
import software.consistent.data.type.solvers.ValuePartition.Number

class AnyNumberSpec : DescribeSpec({
  describe(AnyNumber::class.simpleName!!) {
    it("contains any number") {
      checkAll(Arb.value.number()) { number ->
        expect(AnyNumber()).toContain(number)
      }
    }

    withIts(
      nameFn = { "contains no ${it.name}" },
      ValuePartition.entries.filter { it != Number }) { valuePartition ->
      checkAll(Arb.value(valuePartition)) { value ->
        expect(AnyNumber()).notToContain(value)
      }
    }
  }
})

