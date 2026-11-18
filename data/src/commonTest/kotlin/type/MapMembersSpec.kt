package software.consistent.data.type

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec
import software.consistent.data.type.Map.Component.Companion.optional
import software.consistent.data.type.Map.Component.Companion.required
import software.consistent.data.value.Value

/** Construction is internal for now; types and membership assertions use the public API. */
class MapMembersSpec :
    DescribeSpec(
        {
          val a = Value.Symbol("a")
          val b = Value.Symbol("b")
          val one = Value.Number(1)
          val two = Value.Number(2)
          val three = Value.Number(3)
          val noValues = AnySymbol() and AnyNumber()

          fun member(type: Type): Value.Map {
            val result = checkNotNull(type.solver.anyMember()) as Value.Map
            check(type.allows(result)) { "Constructed member $result is outside $type" }
            return result
          }

          it("omits optional keys and unconstrained remainders") {
            expect(member(Map.Open(optional(AnySymbol() to AnyNumber()))).size).toEqual(0)
            expect(member(Map.Open()).size).toEqual(0)
          }

          it("fills required keys without negative components") {
            expect(member(Map.Closed(required((Just(a) or Just(b)) to Just(one)))).size).toEqual(2)
          }

          it("fills required positives after early builder success") {
            val type =
                Map.Closed(required(Just(a) to Just(one)), required(Just(b) to Just(two))) butNot
                    Map.Open(optional(Just(a) to Just(two)))
            expect(member(type)).toEqual(Value.Map(a to one, b to two))
          }

          it("narrows the same symbolic key through successive continuations") {
            val type =
                Map.Closed(required(Just(a) to (Just(one) or Just(two) or Just(three)))) butNot
                    Map.Open(optional(Just(a) to Just(one))) butNot
                    Map.Open(optional(Just(a) to Just(two)))
            expect(member(type)).toEqual(Value.Map(a to three))
          }

          it("shares an iterator between original and continuation candidates") {
            val keys = Just(a) or Just(b)
            var type: Type = Map.Closed(optional(keys to (Just(one) or Just(two) or Just(three))))
            for (value in listOf(one, two, three)) {
              type = type butNot Map.Open(optional(keys to Just(value)))
            }
            expect(member(type).size).toEqual(2)
          }

          it("leaves an absence witness distinct from a present witness") {
            val keys = Just(a) or Just(b)
            val type =
                Map.Closed(optional(keys to Just(one))) butNot
                    Map.Open(required(keys to Just(one))) butNot
                    Map.Open(optional(keys to noValues))
            expect(member(type).size).toEqual(1)
          }

          for (keys in listOf(AnySymbol(), AnyNumber(), GreaterThan(one) and LessThan(two))) {
            it("takes distinct keys from infinite $keys") {
              val type =
                  Map.Closed(optional(keys to (Just(one) or Just(two)))) butNot
                      Map.Open(optional(keys to Just(one))) butNot
                      Map.Open(optional(keys to Just(two)))
              expect(member(type).size).toEqual(2)
            }
          }

          it("constructs nested map values") {
            val nested = Map.Closed(required(Just(b) to Just(two)))
            member(Map.Closed(required(Just(a) to nested)))
          }

          it("enumerates finite map key types and reuses a key through continuations") {
            val keys = Map.Closed(required(Just(a) to (Just(one) or Just(two))))
            var type: Type = Map.Closed(optional(keys to (Just(one) or Just(two) or Just(three))))
            for (value in listOf(one, two, three)) {
              type = type butNot Map.Open(optional(keys to Just(value)))
            }
            expect(member(type).size).toEqual(2)
          }

          it("takes distinct keys from an infinite map key type") {
            val keys = Map.Closed(required(Just(a) to AnyNumber()))
            val type =
                Map.Closed(optional(keys to (Just(one) or Just(two)))) butNot
                    Map.Open(optional(keys to Just(one))) butNot
                    Map.Open(optional(keys to Just(two)))
            expect(member(type).size).toEqual(2)
          }

          it("enumerates a finite decision node completely without duplicates") {
            val type = Map.Closed(optional((Just(a) or Just(b)) to (Just(one) or Just(two))))
            val results = type.solver.members.toList()
            expect(results.size).toEqual(9)
            expect(results.toSet().size).toEqual(9)
            results.forEach { expect(type).toAllow(it) }
          }

          it("enumerates nested map values without duplicates") {
            val nested = Map.Closed(optional(Just(b) to Just(one)))
            val type = Map.Closed(required(Just(a) to nested))
            val results = type.solver.members.toList()
            expect(results.size).toEqual(2)
            expect(results.toSet().size).toEqual(2)
            results.forEach { expect(type).toAllow(it) }
          }

          it("counts nested map key types only up to the requested bound") {
            val type = Map.Closed(optional(Just(a) to (Just(one) or Just(two))))
            expect(type.solver.cardinality(0).lowerBound).toEqual(1)
            expect(type.solver.cardinality(2).lowerBound).toEqual(3)
            expect(type.solver.cardinality(3).exactValue).toEqual(3)
          }

          it("enumerates map top and individual components lazily") {
            for (type in listOf(Map.Open(), Map.Open(required(Just(a) to Just(one))))) {
              val results = type.solver.members.take(4).toList()
              expect(results.toSet().size).toEqual(4)
              results.forEach { expect(type).toAllow(it) }
            }
          }

          it("returns null for an empty conjunction") {
            val keys = Just(a) or Just(b)
            val values = Just(one) or Just(two) or Just(three)
            var type: Type = Map.Closed(required(keys to values))
            for (value in listOf(one, two, three)) {
              type = type butNot Map.Open(optional(keys to (values butNot Just(value))))
            }
            expect(type.solver.anyMember()).toEqual(null)
          }
        },
    )
