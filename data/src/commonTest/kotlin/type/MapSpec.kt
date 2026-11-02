package software.consistent.data.type

import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.datatest.withDescribes
import software.consistent.data.type.Map.Component.Companion.optional
import software.consistent.data.type.Map.Component.Companion.required
import software.consistent.data.value.Value

class MapSpec : DescribeSpec({
  describe(Map::class.simpleName!!) {
    val a = Value.Symbol("a")
    val b = Value.Symbol("b")
    val c = Value.Symbol("c")
    val one = Value.Number(1)
    val two = Value.Number(2)
    val noValues = AnySymbol() and AnyNumber()

    // Components have the same meaning in open and closed maps; only uncovered keys differ.

    withDescribes(
      mapOf<String, (List<Map.Component>) -> Type>(
        "Open" to Map.Open::invoke,
        "Closed" to Map.Closed::invoke
      )
    ) { mapType ->
      it("contains no scalar values") {
        expect(mapType(emptyList())).toForbid(a).toForbid(one)
      }

      describe("a required key") {
        val type = mapType(listOf(required(Just(a) to AnyNumber())))

        it("accepts a matching value") {
          expect(type).toAllow(Value.Map(a to one))
        }

        it("rejects a missing key") {
          expect(type).toForbid(Value.Map())
        }

        it("rejects a value of the wrong type") {
          expect(type).toForbid(Value.Map(a to b))
        }
      }

      describe("an optional key") {
        val type = mapType(listOf(optional(Just(a) to AnyNumber())))

        it("accepts an absent key") {
          expect(type).toAllow(Value.Map())
        }

        it("accepts a matching value") {
          expect(type).toAllow(Value.Map(a to one))
        }

        it("rejects a present value of the wrong type") {
          expect(type).toForbid(Value.Map(a to b))
        }
      }

      it("requires every key in a union of required keys") {
        expect(mapType(listOf(required((Just(a) or Just(b)) to AnyNumber()))))
          .toForbid(Value.Map())
          .toForbid(Value.Map(a to one))
          .toForbid(Value.Map(b to two))
          .toAllow(Value.Map(a to one, b to two))
          .toForbid(Value.Map(a to c, b to two))
          .toForbid(Value.Map(a to one, b to c))
      }

      it("allows any subset of a union of optional keys") {
        expect(mapType(listOf(optional((Just(a) or Just(b)) to AnyNumber()))))
          .toAllow(Value.Map())
          .toAllow(Value.Map(a to one))
          .toAllow(Value.Map(b to two))
          .toAllow(Value.Map(a to one, b to two))
          .toForbid(Value.Map(a to one, b to c))
      }

      it("combines independent required and optional components") {
        expect(mapType(listOf(required(Just(a) to AnyNumber()), optional(Just(b) to AnySymbol()))))
          .toAllow(Value.Map(a to one))
          .toAllow(Value.Map(a to one, b to c))
          .toForbid(Value.Map(b to c))
          .toForbid(Value.Map(a to one, b to two))
      }

      it("intersects the value types of overlapping components") {
        expect(mapType(listOf(optional((Just(a) or Just(b)) to AnyNumber()), required(Just(a) to Just(one)))))
          .toAllow(Value.Map(a to one))
          .toAllow(Value.Map(a to one, b to two))
          .toForbid(Value.Map())
          .toForbid(Value.Map(b to two))
          .toForbid(Value.Map(a to two))
          .toForbid(Value.Map(a to one, b to c))
      }

      describe("a key constrained by two components") {
        val d = Value.Symbol("d")
        val three = Value.Number(3)
        // b is the only key that both components constrain.
        val firstKeys = Just(b) or Just(c)
        val secondKeys = Just(b) or Just(d)
        val oneOrTwo = Just(one) or Just(two)
        val twoOrThree = Just(two) or Just(three)

        // A component for the unrelated key a must not change the result, even if it has the same value type as one of the
        // other components.
        withData(mapOf(
          "without another key" to (emptyList<Map.Component>() to emptyList()),
          "with another key of the first value type" to
            (listOf(required(Just(a) to oneOrTwo)) to listOf(required(Just(a) to Just(two)))),
          "with another key of the second value type" to
            (listOf(required(Just(a) to twoOrThree)) to listOf(required(Just(a) to Just(one)))),
        )) { (overlappingOthers, disjointOthers) ->
          it("accepts only values that both components allow") {
            val type = mapType(overlappingOthers + required(firstKeys to oneOrTwo) + required(secondKeys to twoOrThree))
            val otherEntries = if (overlappingOthers.isEmpty()) emptyList() else listOf(a to two)

            expect(type).toAllow(Value.Map(otherEntries + listOf(b to two, c to one, d to three)))
            expect(type).toBeSubtypeOf(Map.Open(required(Just(b) to Just(two))))
          }

          it("has no members if the components allow no common value") {
            expect(mapType(disjointOthers + required(firstKeys to Just(two)) + required(secondKeys to Just(one)))).toBeSubtypeOf(noValues)
          }
        }
      }

      describe("required keys") {
        val components = listOf(required(Just(a) to Just(one)), required(Just(b) to Just(two)), required(Just(c) to Just(one)))
        val type = mapType(components)

        withData(components.associateBy { it.toString() }) { component ->
          it("allows no other values at its required keys") {
            expect(type).toBeSubtypeOf(Map.Open(optional(component.key to component.value)))
          }
        }
      }

      it("forbids an optional key whose value constraints are disjoint") {
        expect(mapType(listOf(optional(Just(a) to AnyNumber()), optional(Just(a) to AnySymbol()))))
          .toAllow(Value.Map())
          .toForbid(Value.Map(a to one))
          .toForbid(Value.Map(a to b))
      }

      it("has no members when a required key has disjoint value constraints") {
        val type = mapType(listOf(required(Just(a) to AnyNumber()), optional(Just(a) to AnySymbol())))
        expect(type)
          .toForbid(Value.Map())
          .toForbid(Value.Map(a to one))
          .toForbid(Value.Map(a to b))
        expect(type).toBeSubtypeOf(noValues)
      }

      it("forbids an optional key with an empty value type") {
        expect(mapType(listOf(optional(Just(a) to noValues))))
          .toAllow(Value.Map())
          .toForbid(Value.Map(a to one))
      }

      it("has no members when a required key has an empty value type") {
        expect(mapType(listOf(required(Just(a) to noValues)))).toBeSubtypeOf(noValues)
      }

      it("ignores components with an empty key type") {
        val type = mapType(listOf(required(noValues to noValues), optional(noValues to AnyNumber())))
        expect(type).toAllow(Value.Map()).toBeSubtypeOf(mapType(emptyList()))
        expect(mapType(emptyList())).toBeSubtypeOf(type)
      }

      it("supports numbers as keys") {
        expect(mapType(listOf(required(Just(one) to AnySymbol()))))
          .toAllow(Value.Map(one to a))
          .toForbid(Value.Map())
          .toForbid(Value.Map(one to two))
      }

      it("supports maps as values") {
        expect(mapType(listOf(required(Just(a) to Map.Closed(required(Just(b) to AnyNumber()))))))
          .toAllow(Value.Map(a to Value.Map(b to two)))
          .toForbid(Value.Map(a to Value.Map()))
          .toForbid(Value.Map(a to Value.Map(b to c)))
          .toForbid(Value.Map(a to two))
      }
    }

    describe("uncovered keys") {
      it("are unrestricted in an open map without components") {
        expect(Map.Open())
          .toAllow(Value.Map())
          .toAllow(Value.Map(a to one, two to b))
          .toAllow(Value.Map(Value.Map(a to one) to Value.Map(b to two)))
      }

      it("are forbidden in a closed map without components") {
        expect(Map.Closed())
          .toAllow(Value.Map())
          .toForbid(Value.Map(a to one))
          .toForbid(Value.Map(one to a))
      }

      it("are allowed alongside required keys in an open map") {
        expect(Map.Open(required((Just(a) or Just(b)) to AnyNumber())))
          .toAllow(Value.Map(a to one, b to two, two to c))
      }

      it("are forbidden alongside required keys in a closed map") {
        expect(Map.Closed(required((Just(a) or Just(b)) to AnyNumber())))
          .toForbid(Value.Map(a to one, b to two, two to c))
      }

      it("remain unrestricted outside an infinite optional key type") {
        expect(Map.Open(optional(AnySymbol() to AnyNumber())))
          .toAllow(Value.Map())
          .toAllow(Value.Map(a to one, b to two))
          .toAllow(Value.Map(one to b))
          .toForbid(Value.Map(a to b))
      }

      it("are forbidden outside an infinite optional key type in a closed map") {
        expect(Map.Closed(optional(AnySymbol() to AnyNumber())))
          .toAllow(Value.Map())
          .toAllow(Value.Map(a to one, b to two))
          .toForbid(Value.Map(one to two))
          .toForbid(Value.Map(a to b))
      }
    }

    describe("the remainder of an open map") {
      it("restricts every uncovered value without requiring entries") {
        expect(Map.Open(remainder = AnyNumber()))
          .toAllow(Value.Map())
          .toAllow(Value.Map(a to one, two to one))
          .toForbid(Value.Map(a to b))
          .toForbid(Value.Map(a to one, two to b))
      }

      it("applies only to keys outside the components") {
        expect(Map.Open(required(Just(a) to AnySymbol()), optional(Just(b) to AnySymbol()), remainder = AnyNumber()))
          .toAllow(Value.Map(a to c))
          .toAllow(Value.Map(a to c, b to c, c to one))
          .toForbid(Value.Map(a to one))
          .toForbid(Value.Map(a to c, b to one))
          .toForbid(Value.Map(a to c, c to b))
      }

      it("forbids uncovered keys when the remainder is empty") {
        expect(Map.Open(optional(Just(a) to AnyNumber()), remainder = noValues))
          .toAllow(Value.Map())
          .toAllow(Value.Map(a to one))
          .toForbid(Value.Map(b to two))
      }
    }

    describe("inhabitation with competing negative components") {
      it("shares one key between several negative components") {
        val three = Value.Number(3)
        val positive = Map.Closed(required(Just(a) to (Just(one) or Just(two) or Just(three))))
        val remaining = positive butNot Map.Open(optional(Just(a) to Just(one))) butNot
          Map.Open(optional(Just(a) to Just(two)))

        expect(remaining).notToBeSubtypeOf(noValues)
        expect(remaining).toAllow(Value.Map(a to three)).toForbid(Value.Map(a to one)).toForbid(Value.Map(a to two))
      }

      it("distributes three negative components over two keys") {
        val three = Value.Number(3)
        val keys = Just(a) or Just(b)
        var remaining: Type = Map.Closed(required(keys to (Just(one) or Just(two) or Just(three))))
        for (value in listOf(one, two, three)) {
          remaining = remaining butNot Map.Open(optional(keys to Just(value)))
        }

        // a=1 witnesses the exclusions of {2} and {3}; b=2 witnesses the exclusion of {1}.
        expect(remaining).notToBeSubtypeOf(noValues)
        expect(remaining).toAllow(Value.Map(a to one, b to two)).toForbid(Value.Map(a to one, b to one))
      }

      it("rejects three incompatible witness requirements on two keys") {
        val three = Value.Number(3)
        val keys = Just(a) or Just(b)
        val values = Just(one) or Just(two) or Just(three)
        var remaining: Type = Map.Closed(required(keys to values))
        for (value in listOf(one, two, three)) {
          // Violating this component requires an occurrence of this particular value.
          remaining = remaining butNot Map.Open(optional(keys to (values butNot Just(value))))
        }

        // All three values would have to occur, but a map has only one value per key.
        expect(remaining).toBeSubtypeOf(noValues)
      }

      it("rejects a conjunction with an unwitnessable negative component") {
        val positive = Map.Closed(required(Just(a) to Just(one)), required(Just(b) to Just(one)))
        val remaining = positive butNot Map.Open(optional(Just(a) to Just(two))) butNot
          Map.Open(optional(Just(b) to (Just(one) or Just(two))))

        // a=1 violates the first component; no allowed value at b violates the second.
        expect(remaining).toBeSubtypeOf(noValues)
      }

      it("uses another partition to resolve competing witnesses") {
        val positive = Map.Closed(required(Just(a) to (Just(one) or Just(two))), required(Just(b) to Just(two)))
        val remaining = positive butNot Map.Open(optional((Just(a) or Just(b)) to Just(one))) butNot
          Map.Open(optional(Just(a) to Just(two)))

        // At a, the two obligations compete. b=2 handles the first, leaving a=1 to handle the second.
        expect(remaining).notToBeSubtypeOf(noValues)
        expect(remaining).toAllow(Value.Map(a to one, b to two)).toForbid(Value.Map(a to two, b to two))
      }

      for (keysRequired in listOf(false, true)) {
        it("agrees with exhaustive membership for competing obligations, required keys = $keysRequired") {
          val keys = listOf(a, b, c)
          val positive = Map.Closed(keys.map {
            if (keysRequired) required(Just(it) to (Just(one) or Just(two)))
            else optional(Just(it) to (Just(one) or Just(two)))
          })
          val exclusions = buildList {
            for ((left, right) in listOf(a to b, b to c, c to a)) {
              for (value in listOf(one, two)) {
                add(Map.Open(optional((Just(left) or Just(right)) to Just(value))))
              }
            }
            // Required negative components also allow absence to act as a witness.
            add(Map.Open(required(Just(a) to Just(one))))
            add(Map.Open(required(Just(b) to Just(two))))
          }
          val values = listOf(null, one, two)
          val maps = buildList {
            for (av in values) for (bv in values) for (cv in values) {
              add(Value.Map(keys.zip(listOf(av, bv, cv)).mapNotNull { (key, value) -> value?.let { key to it } }))
            }
          }

          // These 27 maps exhaust the positive type. Membership checks individual maps without invoking the
          // inhabitation search, providing an independent oracle for all 256 subsets of the eight exclusions.
          for (mask in 0 until (1 shl exclusions.size)) {
            val selected = exclusions.filterIndexed { index, _ -> mask and (1 shl index) != 0 }
            val remaining = selected.fold<Type, Type>(positive) { type, excluded -> type butNot excluded }
            val inhabited = maps.any { positive.allows(it) && selected.none { excluded -> excluded.allows(it) } }
            if (inhabited) expect(remaining).notToBeSubtypeOf(noValues)
            else expect(remaining).toBeSubtypeOf(noValues)
          }
        }
      }

      // Each edge requires both values to occur at its endpoints. Each individual negative arrow is satisfiable,
      // but no singleton partition can discharge all its incident obligations. Open negative maps ensure that
      // keys outside an edge cannot witness its negation; the positive map closes the rest of the key universe.
      fun alternatingCycle(keys: List<Value.Symbol>): Type {
        var remaining: Type = Map.Closed(keys.map { required(Just(it) to (Just(one) or Just(two))) })
        for (index in keys.indices) {
          val endpoints = Just(keys[index]) or Just(keys[(index + 1) % keys.size])
          remaining = remaining butNot Map.Open(optional(endpoints to Just(one))) butNot
            Map.Open(optional(endpoints to Just(two)))
        }
        return remaining
      }

      it("finds a consistent assignment around an even cycle of partitions") {
        val d = Value.Symbol("d")
        val remaining = alternatingCycle(listOf(a, b, c, d))

        expect(remaining).notToBeSubtypeOf(noValues)
        expect(remaining)
          .toAllow(Value.Map(a to one, b to two, c to one, d to two))
          .toAllow(Value.Map(a to two, b to one, c to two, d to one))
          .toForbid(Value.Map(a to one, b to two, c to one, d to one))
      }

      it("rejects an odd cycle although every edge can be satisfied on its own") {
        val remaining = alternatingCycle(listOf(a, b, c))

        // Three pairwise adjacent keys cannot alternate between just two values.
        expect(remaining).toBeSubtypeOf(noValues)
      }

      it("rejects incompatible absent and present witnesses at one optional key") {
        val positive = Map.Closed(optional(Just(a) to Just(one)))
        val remaining = positive butNot Map.Open(required(Just(a) to Just(one))) butNot
          Map.Open(optional(Just(a) to noValues))

        // The first negation requires a to be absent; the second requires a to be present.
        expect(remaining).toBeSubtypeOf(noValues)
      }
    }

    describe("type operations") {
      val requiredA = Map.Closed(required(Just(a) to AnyNumber()))
      val optionalA = Map.Closed(optional(Just(a) to AnyNumber()))
      val requiredB = Map.Closed(required(Just(b) to AnySymbol()))

      it("unites alternative map shapes") {
        expect(requiredA or requiredB)
          .toAllow(Value.Map(a to one))
          .toAllow(Value.Map(b to c))
          .toForbid(Value.Map())
          .toForbid(Value.Map(a to one, b to c))
      }

      it("intersects constraints from separate open map types") {
        expect(Map.Open(required(Just(a) to AnyNumber())) and Map.Open(required(Just(b) to AnySymbol())))
          .toAllow(Value.Map(a to one, b to c))
          .toForbid(Value.Map(a to one))
          .toForbid(Value.Map(b to c))
          .toForbid(Value.Map(a to c, b to c))
      }

      it("subtracts maps containing an optional key to leave the empty map") {
        expect(optionalA butNot requiredA)
          .toAllow(Value.Map())
          .toForbid(Value.Map(a to one))
      }

      it("subtracts the empty map to make an optional key required") {
        expect(optionalA butNot Map.Closed())
          .toForbid(Value.Map())
          .toAllow(Value.Map(a to one))
          .toForbid(Value.Map(a to b))
      }

      it("contains a required-key type in the corresponding optional-key type") {
        expect(requiredA).toBeSubtypeOf(optionalA)
        expect(optionalA).notToBeSubtypeOf(requiredA)
      }

      it("contains a closed map type in the corresponding open map type") {
        val open = Map.Open(required(Just(a) to AnyNumber()))
        expect(requiredA).toBeSubtypeOf(open)
        expect(open).notToBeSubtypeOf(requiredA)
      }

      it("contains narrower value types") {
        val narrow = Map.Closed(required(Just(a) to Just(one)))
        expect(narrow).toBeSubtypeOf(requiredA)
        expect(requiredA).notToBeSubtypeOf(narrow)
      }

      it("contains exactly the empty map in a closed map without components") {
        expect(Just(Value.Map())).toBeSubtypeOf(Map.Closed())
        expect(Map.Closed()).toBeSubtypeOf(Just(Value.Map()))
      }

      it("has no members in the intersection of incompatible closed shapes") {
        expect(requiredA and requiredB).toBeSubtypeOf(noValues)
      }

      it("has no members after subtracting itself") {
        expect(requiredA butNot requiredA).toBeSubtypeOf(noValues)
      }
    }
  }
})
