package software.consistent.collections

import ch.tutteli.atrium.api.fluent.en_GB.*
import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.pair
import io.kotest.property.checkAll

class BitSetSpec : DescribeSpec({
  describe("BitSet") {
    describe("creation") {
      it("starts empty") {
        val set = BitSet(100)
        expect(set.size).toEqual(0)
        expect(set.isEmpty()).toEqual(true)
        expect(set.isNotEmpty()).toEqual(false)
        expect(set.elements()).toBeEmpty()
        expect((0..<100).filter { it in set }).toBeEmpty()
      }

      it("starts empty without an initial capacity") {
        val set = BitSet()
        expect(set.isEmpty()).toEqual(true)
        expect(0 in set).toEqual(false)
        expect(set.elements()).toBeEmpty()
      }

      describe("rejects a negative initial capacity") {
        withData(-1, Int.MIN_VALUE) { capacity ->
          expect { BitSet(capacity) }.toThrow<IllegalArgumentException>().message
            .toEqual("The initial capacity must not be negative, but was $capacity.")
        }
      }
    }

    describe("add") {
      it("reports whether the element was new") {
        val set = BitSet(10)
        expect(set.add(3)).toEqual(true)
        expect(set.add(3)).toEqual(false)
        expect(set.size).toEqual(1)
        expect(3 in set).toEqual(true)
      }

      describe("rejects negative elements") {
        withData(-1, Int.MIN_VALUE) { element ->
          val set = BitSet(10)
          expect { set.add(element) }.toThrow<IndexOutOfBoundsException>().message
            .toEqual("Cannot add $element to a BitSet: elements must not be negative.")
          expect(set.size).toEqual(0)
        }
      }

      describe("grows beyond the initial capacity") {
        withData(0, 1, 10, 32, 64) { initialCapacity ->
          val elements = listOf(initialCapacity, 31, 32, 63, 64, 65, 1000, 100_000).distinct().sorted()
          val set = BitSet(initialCapacity)
          elements.forEach { expect(set.add(it)).toEqual(true) }
          expect(set.size).toEqual(elements.size)
          expect(set.elements()).toEqual(elements)
          expect((0..100_001).filter { it in set }).toEqual(elements)
        }
      }

      it("keeps its elements when growing step by step") {
        val set = BitSet()
        (0..<300).forEach { set.add(it) }
        expect(set.elements()).toEqual((0..<300).toList())
      }
    }

    describe("remove") {
      it("reports whether the element was present") {
        val set = BitSet(10)
        set.add(3)
        expect(set.remove(3)).toEqual(true)
        expect(set.remove(3)).toEqual(false)
        expect(set.size).toEqual(0)
        expect(3 in set).toEqual(false)
      }

      describe("ignores negative elements and elements beyond the allocated ones") {
        withData(-1, 10, 1000, Int.MIN_VALUE, Int.MAX_VALUE) { element ->
          val set = BitSet(10).apply { add(9) }
          expect(set.remove(element)).toEqual(false)
          expect(set.elements()).toContainExactly(9)
        }
      }
    }

    describe("contains is false for negative elements and elements beyond the allocated ones") {
      withData(-1, 10, 1000, Int.MIN_VALUE, Int.MAX_VALUE) { element ->
        expect(element in BitSet(10).apply { add(0); add(9) }).toEqual(false)
      }
    }

    it("clear removes all elements and allows adding them again") {
      val set = BitSet(100).apply { add(0); add(50); add(99) }
      set.clear()
      expect(set.size).toEqual(0)
      expect(set.elements()).toBeEmpty()
      expect(set.add(50)).toEqual(true)
      expect(set.elements()).toContainExactly(50)
    }

    describe("plusAssign") {
      it("adds the element") {
        val set = BitSet(10)
        set += 3
        set += 3
        expect(set.elements()).toContainExactly(3)
      }

      it("grows beyond the initial capacity") {
        val set = BitSet(10)
        set += 100
        expect(set.elements()).toContainExactly(100)
      }

      it("rejects negative elements") {
        val set = BitSet(10)
        expect { set += -1 }.toThrow<IndexOutOfBoundsException>()
        expect(set.size).toEqual(0)
      }
    }

    describe("minusAssign") {
      it("removes the element") {
        val set = BitSet(10).apply { add(3); add(5) }
        set -= 3
        set -= 3
        expect(set.elements()).toContainExactly(5)
      }

      it("ignores negative elements and elements beyond the allocated ones") {
        val set = BitSet(10).apply { add(9) }
        set -= 1000
        set -= -1
        expect(set.elements()).toContainExactly(9)
      }
    }

    describe("removeAll") {
      it("removes exactly the matching elements") {
        val set = BitSet(100).apply { listOf(1, 2, 3, 64, 65, 99).forEach { add(it) } }
        expect(set.removeAll { it % 2 == 1 }).toEqual(true)
        expect(set.elements()).toContainExactly(2, 64)
        expect(set.size).toEqual(2)
      }

      it("calls the predicate once for each element, in ascending order") {
        val set = BitSet(100).apply { listOf(70, 0, 31, 32).forEach { add(it) } }
        val visited = mutableListOf<Int>()
        set.removeAll { visited += it; it == 31 }
        expect(visited).toContainExactly(0, 31, 32, 70)
      }

      // Removes the element that forEach is at, in every position of a word and across word boundaries
      it("removes every other element of a full set") {
        val set = BitSet(130).apply { (0..<130).forEach { add(it) } }
        expect(set.removeAll { it % 2 == 0 }).toEqual(true)
        expect(set.elements()).toEqual((1..<130 step 2).toList())
        expect(set.size).toEqual(65)
      }

      it("can remove all elements") {
        val set = BitSet(130).apply { (0..<130).forEach { add(it) } }
        expect(set.removeAll { true }).toEqual(true)
        expect(set.isEmpty()).toEqual(true)
        expect(set.elements()).toBeEmpty()
      }

      it("returns false and keeps all elements if nothing matches") {
        val set = BitSet(10).apply { add(1); add(9) }
        expect(set.removeAll { false }).toEqual(false)
        expect(set.elements()).toContainExactly(1, 9)
      }
    }

    describe("copy") {
      it("has the same elements") {
        val set = BitSet(100).apply { add(0); add(63); add(64); add(99) }
        val copy = set.copy()
        expect(copy.size).toEqual(4)
        expect(copy.elements()).toContainExactly(0, 63, 64, 99)
        expect(copy).toEqual(set)
      }

      it("is independent of the original") {
        val set = BitSet(100).apply { add(1); add(2) }
        val copy = set.copy()
        copy += 3
        copy -= 1
        set += 50
        expect(set.elements()).toContainExactly(1, 2, 50)
        expect(copy.elements()).toContainExactly(2, 3)
        expect(set.size).toEqual(3)
        expect(copy.size).toEqual(2)
      }

      it("is independent of the original after clear") {
        val set = BitSet(10).apply { add(1) }
        val copy = set.copy()
        set.clear()
        expect(copy.elements()).toContainExactly(1)
      }

      it("copies an empty set") {
        val copy = BitSet().copy()
        expect(copy.isEmpty()).toEqual(true)
      }

      it("stays independent when either set grows") {
        val set = BitSet().apply { add(1) }
        val copy = set.copy()
        set += 1000
        copy += 2000
        expect(set.elements()).toContainExactly(1, 1000)
        expect(copy.elements()).toContainExactly(1, 2000)
      }

      it("copies a grown set") {
        val set = BitSet().apply { add(5); add(1000) }
        expect(set.copy().elements()).toContainExactly(5, 1000)
      }
    }

    describe("single") {
      it("returns the only element") {
        expect(BitSet().apply { add(70) }.single()).toEqual(70)
      }

      it("rejects an empty set") {
        expect { BitSet().single() }.toThrow<NoSuchElementException>().message.toEqual("BitSet is empty.")
        expect { BitSet().apply { add(1); remove(1) }.single() }.toThrow<NoSuchElementException>()
      }

      it("rejects a set with more than one element") {
        expect { BitSet().apply { add(1); add(70) }.single() }.toThrow<IllegalArgumentException>().message
          .toEqual("BitSet has more than one element.")
      }
    }

    it("iterates in ascending order, independent of the insertion order") {
      val set = BitSet(200).apply { listOf(150, 3, 64, 0, 199, 31).forEach { add(it) } }
      expect(set.elements()).toContainExactly(0, 3, 31, 64, 150, 199)
    }

    // Each platform stores the bits in words of 32 or 64 bits, so the elements at and around word boundaries are the critical ones.
    describe("handles elements at word boundaries") {
      withData(1, 31, 32, 33, 63, 64, 65, 96, 128, 129, 200) { capacity ->
        val boundaries = listOf(0, 1, 30, 31, 32, 33, 62, 63, 64, 65, 95, 96, 97, 127, 128, capacity - 1)
          .filter { it in 0..<capacity }.distinct().sorted()
        // Once allocated up front, and once grown while adding
        for (set in listOf(BitSet(capacity), BitSet())) {
          boundaries.forEach { set.add(it) }
          expect(set.elements()).toEqual(boundaries)
          expect(set.size).toEqual(boundaries.size)
          expect((0..<capacity).filter { it in set }).toEqual(boundaries)

          boundaries.forEach { expect(set.remove(it)).toEqual(true) }
          expect(set.elements()).toBeEmpty()
        }
      }
    }

    it("iterates over a single element in each position of a word") {
      for (element in 0..<130) {
        val set = BitSet(130).apply { add(element) }
        expect(set.elements()).toContainExactly(element)
      }
    }

    it("iterates over all elements of a full set") {
      val set = BitSet(130)
      (0..<130).forEach { set.add(it) }
      expect(set.elements()).toEqual((0..<130).toList())
    }

    it("behaves like a sorted set of ints") {
      checkAll(
        iterations = 200,
        Arb.int(0..300),
        Arb.operations(),
      ) { initialCapacity, operations ->
        val set = BitSet(initialCapacity)
        val model = mutableSetOf<Int>()
        for ((add, element) in operations) {
          if (add) expect(set.add(element)).toEqual(model.add(element))
          else expect(set.remove(element)).toEqual(model.remove(element))
        }
        expect(set.size).toEqual(model.size)
        expect(set.elements()).toEqual(model.sorted())
        expect((0..<maxElement + 2).filter { it in set }).toEqual(model.sorted())
        expect(set.toString()).toEqual(model.sorted().toString())
        expect(set.hashCode()).toEqual(model.hashCode())

        val copy = set.copy()
        expect(copy.removeAll { it % 3 == 0 }).toEqual(model.any { it % 3 == 0 })
        expect(copy.elements()).toEqual(model.filter { it % 3 != 0 }.sorted())
        expect(copy.size).toEqual(model.count { it % 3 != 0 })
        expect(set.elements()).toEqual(model.sorted())
      }
    }

    describe("equals & hashCode") {
      it("considers sets with the same elements equal, regardless of their initial capacities") {
        val set = BitSet(10).apply { add(1); add(5) }
        val other = BitSet(100).apply { add(5); add(1) }
        expect(set).toEqual(other)
        expect(set.hashCode()).toEqual(other.hashCode())
      }

      it("considers empty sets equal") {
        expect(BitSet(0)).toEqual(BitSet(64))
      }

      it("considers sets with different elements unequal") {
        expect(BitSet(10).apply { add(1) }).notToEqual(BitSet(10).apply { add(2) })
        expect(BitSet(10).apply { add(1) }).notToEqual(BitSet(10).apply { add(1); add(2) })
      }

      it("is not equal to a Set with the same elements") {
        expect<Any>(BitSet(10).apply { add(1) }).notToEqual(setOf(1))
      }

      it("has the hash code of a Set with the same elements") {
        expect(BitSet(100).apply { add(3); add(64); add(99) }.hashCode()).toEqual(setOf(3, 64, 99).hashCode())
      }
    }

    describe("toString") {
      it("lists the elements like a Set") {
        expect(BitSet(100).apply { add(64); add(1); add(5) }.toString()).toEqual("[1, 5, 64]")
      }

      it("prints an empty set like an empty Set") {
        expect(BitSet(10).toString()).toEqual("[]")
      }
    }
  }
})

private fun BitSet.elements(): List<Int> = buildList { this@elements.forEach { add(it) } }

private const val maxElement = 700

/**
 * Additions (`true`) and removals (`false`) of elements up to [maxElement], with many repeated elements. The elements exceed the
 * initial capacities of the property test, so that the sets grow.
 */
private fun Arb.Companion.operations(): Arb<List<Pair<Boolean, Int>>> = list(pair(boolean(), int(0..maxElement)), 0..400)
