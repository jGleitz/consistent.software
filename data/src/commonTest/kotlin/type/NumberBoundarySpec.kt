package software.consistent.data.type

import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withDescribes
import software.consistent.data.value.Number

class NumberBoundarySpec : DescribeSpec({
  describe("number boundaries") {
    val boundaryTypes = listOf(
      NumberBoundary(LessThan::invoke) { value, boundary -> value < boundary },
      NumberBoundary(LessThanOrEqualTo::invoke) { value, boundary -> value <= boundary },
      NumberBoundary(GreaterThan::invoke) { value, boundary -> value > boundary },
      NumberBoundary(GreaterThanOrEqualTo::invoke) { value, boundary -> value >= boundary },
    )
    val thresholds = listOf(-1, 0, 1).map { Number(it) }
    val values = listOf(-2, -1, 0, 1, 2).map { Number(it) }

    val boundaries = boundaryTypes.flatMap { boundary ->
      thresholds.map { threshold -> boundary.at(threshold) }
    }
    val cases = boundaries.flatMap { left ->
      boundaries.map { right -> left to right }
    }

    describe("union") {
      withDescribes(nameFn = { (left, right) -> "${left.type} | ${right.type}" }, cases) { (left, right) ->
        val union = left.type or right.type

        it("contains the expected values") {
          expect(union) {
            for (value in values) {
              if (left.allows(value) || right.allows(value)) toAllow(value)
              else toForbid(value)
            }
          }
        }
      }
    }

    describe("intersection") {
      withDescribes(nameFn = { (left, right) -> "${left.type} & ${right.type}" }, cases) { (left, right) ->
        val intersection = left.type and right.type

        it("contains the expected values") {
          expect(intersection) {
            for (value in values) {
              if (left.allows(value) && right.allows(value)) toAllow(value)
              else toForbid(value)
            }
          }
        }
      }
    }
  }
})

private class NumberBoundary(
  private val create: (Number) -> Type,
  private val predicate: (Number, Number) -> Boolean,
) {
  fun at(threshold: Number) = NumberBoundaryCase(create(threshold)) { value -> predicate(value, threshold) }
}

private class NumberBoundaryCase(val type: Type, val allows: (Number) -> Boolean)
