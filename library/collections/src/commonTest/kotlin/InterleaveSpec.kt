package software.consistent.collections

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec

class InterleaveSpec :
    DescribeSpec(
        {
          describe("interleave") {
            it("returns an empty sequence for no inputs") {
              expect(interleave<Int>(emptyList()).toList()).toEqual(emptyList())
            }

            it("returns an empty sequence when all inputs are empty") {
              expect(
                      interleave(listOf(emptySequence<Int>(), emptySequence(), emptySequence()))
                          .toList(),
                  )
                  .toEqual(emptyList())
            }

            it("preserves a single finite input") {
              expect(interleave(listOf(sequenceOf(1, 2, 3))).toList()).toEqual(listOf(1, 2, 3))
            }

            it("takes turns across equally sized finite inputs") {
              val sources = listOf(sequenceOf(1, 2), sequenceOf(10, 20), sequenceOf(100, 200))
              expect(interleave(sources).toList()).toEqual(listOf(1, 10, 100, 2, 20, 200))
            }

            it("continues after shorter inputs are exhausted") {
              val sources = listOf(sequenceOf(1, 2, 3), sequenceOf(10), sequenceOf(100, 200))
              expect(interleave(sources).toList()).toEqual(listOf(1, 10, 100, 2, 200, 3))
            }

            it("skips empty inputs at the beginning, middle and end") {
              val sources =
                  listOf(
                      emptySequence(),
                      sequenceOf(1, 2),
                      emptySequence(),
                      emptySequence(),
                      sequenceOf(10, 20),
                      emptySequence(),
                  )
              expect(interleave(sources).toList()).toEqual(listOf(1, 10, 2, 20))
            }

            it("preserves duplicates and nullable elements") {
              val sources = listOf(sequenceOf(null, 1, 1), sequenceOf(null, 2))
              expect(interleave(sources).toList()).toEqual(listOf(null, null, 1, 2, 1))
            }

            it("takes turns across infinite inputs") {
              val sources = listOf(generateSequence(0) { it + 1 }, generateSequence(100) { it + 1 })
              expect(interleave(sources).take(6).toList()).toEqual(listOf(0, 100, 1, 101, 2, 102))
            }

            it("continues infinite inputs after a finite input ends") {
              val sources =
                  listOf(
                      generateSequence(0) { it + 1 },
                      sequenceOf(10, 20),
                      generateSequence(100) { it + 1 },
                  )
              expect(interleave(sources).take(10).toList())
                  .toEqual(listOf(0, 10, 100, 1, 20, 101, 2, 102, 3, 103))
            }

            it("skips empty inputs alongside an infinite input") {
              val sources = listOf(emptySequence(), generateSequence(0) { it + 1 }, emptySequence())
              expect(interleave(sources).take(4).toList()).toEqual(listOf(0, 1, 2, 3))
            }

            it("defers creating input iterators until consumption") {
              var iteratorsCreated = 0
              val source = Sequence {
                iteratorsCreated += 1
                listOf(1, 2).iterator()
              }
              val result = interleave(listOf(source))
              val iterator = result.iterator()
              expect(iteratorsCreated).toEqual(0)
              expect(iterator.hasNext()).toEqual(true)
              expect(iteratorsCreated).toEqual(1)
              expect(iterator.next()).toEqual(1)
            }

            it("consumes only the requested prefix") {
              var firstReads = 0
              var secondReads = 0
              val first = generateSequence(0) { it + 1 }.onEach { firstReads += 1 }
              val second = generateSequence(100) { it + 1 }.onEach { secondReads += 1 }
              expect(interleave(listOf(first, second)).take(1).toList()).toEqual(listOf(0))
              expect(firstReads).toEqual(1)
              expect(secondReads).toEqual(0)
            }

            it("keeps result iterators independent when inputs are repeatable") {
              val result = interleave(listOf(sequenceOf(1, 2), sequenceOf(10)))
              val first = result.iterator()
              val second = result.iterator()
              expect(first.next()).toEqual(1)
              expect(first.next()).toEqual(10)
              expect(second.next()).toEqual(1)
              expect(first.next()).toEqual(2)
              expect(first.hasNext()).toEqual(false)
              expect(second.asSequence().toList()).toEqual(listOf(10, 2))
            }
          }
        },
    )
