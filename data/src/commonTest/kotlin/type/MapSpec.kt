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
    val c = Value.Symbol("c")
    val one = Value.Number(1)
    val two = Value.Number(2)
    val noValues = AnySymbol() and AnyNumber()

    // Components have the same meaning in open and closed maps; only uncovered keys differ.
    val constructors: List<Pair<String, (List<Map.Component>) -> Type>> = listOf(
      "Open" to Map.Open::invoke,
      "Closed" to Map.Closed::invoke
    )

    for ((name, mapType) in constructors) {
      describe(name) {
        it("contains no scalar values") {
          expect(mapType(emptyList())).notToContain(a).notToContain(one)
        }

        describe("a required key") {
          val type = mapType(listOf(required(Just(a) to AnyNumber())))

          it("accepts a matching value") {
            expect(type).toContain(Value.Map(a to one))
          }

          it("rejects a missing key") {
            expect(type).notToContain(Value.Map())
          }

          it("rejects a value of the wrong type") {
            expect(type).notToContain(Value.Map(a to b))
          }
        }

        describe("an optional key") {
          val type = mapType(listOf(optional(Just(a) to AnyNumber())))

          it("accepts an absent key") {
            expect(type).toContain(Value.Map())
          }

          it("accepts a matching value") {
            expect(type).toContain(Value.Map(a to one))
          }

          it("rejects a present value of the wrong type") {
            expect(type).notToContain(Value.Map(a to b))
          }
        }

        it("requires every key in a union of required keys") {
          expect(mapType(listOf(required((Just(a) or Just(b)) to AnyNumber()))))
            .notToContain(Value.Map())
            .notToContain(Value.Map(a to one))
            .notToContain(Value.Map(b to two))
            .toContain(Value.Map(a to one, b to two))
            .notToContain(Value.Map(a to c, b to two))
            .notToContain(Value.Map(a to one, b to c))
        }

        it("allows any subset of a union of optional keys") {
          expect(mapType(listOf(optional((Just(a) or Just(b)) to AnyNumber()))))
            .toContain(Value.Map())
            .toContain(Value.Map(a to one))
            .toContain(Value.Map(b to two))
            .toContain(Value.Map(a to one, b to two))
            .notToContain(Value.Map(a to one, b to c))
        }

        it("combines independent required and optional components") {
          expect(mapType(listOf(required(Just(a) to AnyNumber()), optional(Just(b) to AnySymbol()))))
            .toContain(Value.Map(a to one))
            .toContain(Value.Map(a to one, b to c))
            .notToContain(Value.Map(b to c))
            .notToContain(Value.Map(a to one, b to two))
        }

        it("intersects the value types of overlapping components") {
          expect(mapType(listOf(optional((Just(a) or Just(b)) to AnyNumber()), required(Just(a) to Just(one)))))
            .toContain(Value.Map(a to one))
            .toContain(Value.Map(a to one, b to two))
            .notToContain(Value.Map())
            .notToContain(Value.Map(b to two))
            .notToContain(Value.Map(a to two))
            .notToContain(Value.Map(a to one, b to c))
        }

        it("forbids an optional key whose value constraints are disjoint") {
          expect(mapType(listOf(optional(Just(a) to AnyNumber()), optional(Just(a) to AnySymbol()))))
            .toContain(Value.Map())
            .notToContain(Value.Map(a to one))
            .notToContain(Value.Map(a to b))
        }

        it("has no members when a required key has disjoint value constraints") {
          val type = mapType(listOf(required(Just(a) to AnyNumber()), optional(Just(a) to AnySymbol())))
          expect(type)
            .notToContain(Value.Map())
            .notToContain(Value.Map(a to one))
            .notToContain(Value.Map(a to b))
          expect(noValues).toContain(type)
        }

        it("forbids an optional key with an empty value type") {
          expect(mapType(listOf(optional(Just(a) to noValues))))
            .toContain(Value.Map())
            .notToContain(Value.Map(a to one))
        }

        it("has no members when a required key has an empty value type") {
          expect(noValues).toContain(mapType(listOf(required(Just(a) to noValues))))
        }

        it("ignores components with an empty key type") {
          expect(mapType(listOf(required(noValues to noValues), optional(noValues to AnyNumber()))))
            .toContain(Value.Map())
        }

        it("supports numbers as keys") {
          expect(mapType(listOf(required(Just(one) to AnySymbol()))))
            .toContain(Value.Map(one to a))
            .notToContain(Value.Map())
            .notToContain(Value.Map(one to two))
        }

        it("supports maps as values") {
          expect(mapType(listOf(required(Just(a) to Map.Closed(required(Just(b) to AnyNumber()))))))
            .toContain(Value.Map(a to Value.Map(b to two)))
            .notToContain(Value.Map(a to Value.Map()))
            .notToContain(Value.Map(a to Value.Map(b to c)))
            .notToContain(Value.Map(a to two))
        }
      }
    }

    describe("uncovered keys") {
      it("are unrestricted in an open map without components") {
        expect(Map.Open())
          .toContain(Value.Map())
          .toContain(Value.Map(a to one, two to b))
          .toContain(Value.Map(Value.Map(a to one) to Value.Map(b to two)))
      }

      it("are forbidden in a closed map without components") {
        expect(Map.Closed())
          .toContain(Value.Map())
          .notToContain(Value.Map(a to one))
          .notToContain(Value.Map(one to a))
      }

      it("are allowed alongside required keys in an open map") {
        expect(Map.Open(required((Just(a) or Just(b)) to AnyNumber())))
          .toContain(Value.Map(a to one, b to two, two to c))
      }

      it("are forbidden alongside required keys in a closed map") {
        expect(Map.Closed(required((Just(a) or Just(b)) to AnyNumber())))
          .notToContain(Value.Map(a to one, b to two, two to c))
      }

      it("remain unrestricted outside an infinite optional key type") {
        expect(Map.Open(optional(AnySymbol() to AnyNumber())))
          .toContain(Value.Map())
          .toContain(Value.Map(a to one, b to two))
          .toContain(Value.Map(one to b))
          .notToContain(Value.Map(a to b))
      }

      it("are forbidden outside an infinite optional key type in a closed map") {
        expect(Map.Closed(optional(AnySymbol() to AnyNumber())))
          .toContain(Value.Map())
          .toContain(Value.Map(a to one, b to two))
          .notToContain(Value.Map(one to two))
          .notToContain(Value.Map(a to b))
      }
    }

    describe("the remainder of an open map") {
      it("restricts every uncovered value without requiring entries") {
        expect(Map.Open(remainder = AnyNumber()))
          .toContain(Value.Map())
          .toContain(Value.Map(a to one, two to one))
          .notToContain(Value.Map(a to b))
          .notToContain(Value.Map(a to one, two to b))
      }

      it("applies only to keys outside the components") {
        expect(Map.Open(required(Just(a) to AnySymbol()), optional(Just(b) to AnySymbol()), remainder = AnyNumber()))
          .toContain(Value.Map(a to c))
          .toContain(Value.Map(a to c, b to c, c to one))
          .notToContain(Value.Map(a to one))
          .notToContain(Value.Map(a to c, b to one))
          .notToContain(Value.Map(a to c, c to b))
      }

      it("forbids uncovered keys when the remainder is empty") {
        expect(Map.Open(optional(Just(a) to AnyNumber()), remainder = noValues))
          .toContain(Value.Map())
          .toContain(Value.Map(a to one))
          .notToContain(Value.Map(b to two))
      }
    }

    describe("type operations") {
      val requiredA = Map.Closed(required(Just(a) to AnyNumber()))
      val optionalA = Map.Closed(optional(Just(a) to AnyNumber()))
      val requiredB = Map.Closed(required(Just(b) to AnySymbol()))

      it("unites alternative map shapes") {
        expect(requiredA or requiredB)
          .toContain(Value.Map(a to one))
          .toContain(Value.Map(b to c))
          .notToContain(Value.Map())
          .notToContain(Value.Map(a to one, b to c))
      }

      it("intersects constraints from separate open map types") {
        expect(Map.Open(required(Just(a) to AnyNumber())) and Map.Open(required(Just(b) to AnySymbol())))
          .toContain(Value.Map(a to one, b to c))
          .notToContain(Value.Map(a to one))
          .notToContain(Value.Map(b to c))
          .notToContain(Value.Map(a to c, b to c))
      }

      it("subtracts maps containing an optional key to leave the empty map") {
        expect(optionalA butNot requiredA)
          .toContain(Value.Map())
          .notToContain(Value.Map(a to one))
      }

      it("subtracts the empty map to make an optional key required") {
        expect(optionalA butNot Map.Closed())
          .notToContain(Value.Map())
          .toContain(Value.Map(a to one))
          .notToContain(Value.Map(a to b))
      }

      it("contains a required-key type in the corresponding optional-key type") {
        expect(optionalA).toContain(requiredA)
        expect(requiredA).notToContain(optionalA)
      }

      it("contains a closed map type in the corresponding open map type") {
        val open = Map.Open(required(Just(a) to AnyNumber()))
        expect(open).toContain(requiredA)
        expect(requiredA).notToContain(open)
      }

      it("contains narrower value types") {
        val narrow = Map.Closed(required(Just(a) to Just(one)))
        expect(requiredA).toContain(narrow)
        expect(narrow).notToContain(requiredA)
      }

      it("contains exactly the empty map in a closed map without components") {
        expect(Map.Closed()).toContain(Just(Value.Map()))
        expect(Just(Value.Map())).toContain(Map.Closed())
      }

      it("has no members in the intersection of incompatible closed shapes") {
        expect(noValues).toContain(requiredA and requiredB)
      }

      it("has no members after subtracting itself") {
        expect(noValues).toContain(requiredA butNot requiredA)
      }
    }
  }
})
