package software.consistent.data.type

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec
import software.consistent.data.value.Value

class MembersSpec :
    DescribeSpec(
        {
          val zero = Value.Number(0)
          val one = Value.Number(1)
          val two = Value.Number(2)
          val a = Value.Symbol("a")

          it("enumerates empty and finite scalar sets completely") {
            expect((AnySymbol() and AnyNumber()).solver.members.toList().size).toEqual(0)
            val values = (Just(zero) or Just(one) or Just(a)).solver.members.toList()
            expect(values.toSet()).toEqual(setOf(zero, one, a))
            expect(values.size).toEqual(3)
          }

          val intervals =
              listOf(
                  AnyNumber(),
                  GreaterThan(one),
                  LessThan(one),
                  GreaterThan(one) and LessThan(two),
                  GreaterThanOrEqualTo(one) and LessThanOrEqualTo(two),
                  LessThan(zero) or (GreaterThan(one) and LessThan(two)) or Just(Value.Number(9)),
                  AnyNumber() butNot Just(one),
              )
          for (type in intervals) {
            it("lazily enumerates distinct members of $type") {
              val values = type.solver.members.take(30).toList()
              expect(values.toSet().size).toEqual(30)
              values.forEach { expect(type).toAllow(it) }
              expect(type).toAllow(checkNotNull(type.solver.anyMember()))
            }
          }

          it("enumerates finite numeric singletons only once") {
            val values = (Just(one) or Just(two)).solver.members.toList()
            expect(values.size).toEqual(2)
            expect(values.toSet()).toEqual(setOf(one, two))
          }

          it("enumerates symbol complements without excluded identifiers") {
            val type = AnySymbol() butNot Just(a)
            val values = type.solver.members.take(30).toList()
            expect(values.map { it.toString() }.toSet().size).toEqual(30)
            values.forEach { expect(type).toAllow(it) }
          }

          it("gives independent iterators") {
            val sequence = AnyNumber().solver.members
            expect(sequence.take(10).toList()).toEqual(sequence.take(10).toList())
          }

          it("does not starve later value partitions") {
            val type = AnySymbol() or Just(one) or Map.Closed()
            val values = type.solver.members.take(5).toList()
            expect(values.any { it is Value.Number }).toEqual(true)
            expect(values.any { it is Value.Map }).toEqual(true)
          }
        },
    )
