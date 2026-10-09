package software.consistent.model.type

import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec
import software.consistent.model.type.Map.Component.Companion.optional
import software.consistent.model.type.Map.Component.Companion.required
import software.consistent.model.value.Value

class MapSpec : DescribeSpec({
  describe(Map::class.simpleName!!) {
    val a = Value.Symbol("a")
    val b = Value.Symbol("b")

    it("checks out") {
      expect(Map.Open(optional(AnySymbol(), AnyNumber())))
        .toContain(Value.Map())
        .toContain(Value.Map(Value.Symbol("foo") to Value.Number(1)))
        .toContain(Value.Map(Value.Number(3) to Value.Number(2)))


      expect(Map.Open(required(Just(a) or Just(b), AnyNumber())))
        .notToContain(Value.Map())
        .notToContain(Value.Map(a to Value.Number(1)))
        .notToContain(Value.Map(a to Value.Number(1)))
        .toContain(
          Value.Map(
            a to Value.Number(1),
            b to Value.Number(2)
          )
        )
        .toContain(
          Value.Map(
            a to Value.Number(1),
            b to Value.Number(2),
            Value.Number(42) to Value.Number(33)
          )
        )

      expect(Map.Closed(required(Just(a) or Just(b), AnyNumber())))
        .notToContain(Value.Map())
        .notToContain(Value.Map(a to Value.Number(1)))
        .notToContain(Value.Map(a to Value.Number(1)))
        .toContain(
          Value.Map(
            a to Value.Number(1),
            b to Value.Number(2)
          )
        )
        .notToContain(
          Value.Map(
            a to Value.Number(1),
            b to Value.Number(2),
            Value.Number(42) to Value.Number(33)
          )
        )
    }
  }
})
