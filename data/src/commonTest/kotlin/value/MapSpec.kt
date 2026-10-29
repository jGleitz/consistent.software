package software.consistent.data.value

import ch.tutteli.atrium.api.fluent.en_GB.*
import ch.tutteli.atrium.api.verbs.expect
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withIts
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.checkAll
import software.consistent.data.generators.value

@OptIn(ExperimentalKotest::class)
class MapSpec : DescribeSpec({
  describe("Map") {
    describe("creation") {
      describe("creates equal maps from all factories") {
        withIts(
          mapOf(
            "pairs" to Map(a to one, b to half),
            "a list of pairs" to Map(listOf(a to one, b to half)),
            "a Kotlin map" to Map(mapOf(a to one, b to half)),
          )
        ) { map ->
          expect(map.entryPairs()).toContain.inAnyOrder.only.values(a to one, b to half)
        }
      }

      describe("keeps only the last value of keys that are equal") {
        withIts(
          mapOf(
            "the same symbol" to (Map(a to one, a to two) to (a to two)),
            "equal numbers from different sources" to (Map(Number(2) to one, Number(2.0) to two) to (Number(2) to two)),
            "equal maps" to (Map(Map(a to one) to one, Map(a to one) to two) to (Map(a to one) to two)),
          )
        ) { (map, onlyEntry) ->
          expect(map.entryPairs()).toContain.inAnyOrder.only.values(onlyEntry)
        }
      }

      describe("copies its source, so that later changes to the source don’t affect the map") {
        withIts(
          mapOf<String, () -> Map>(
            "a Kotlin map" to {
              val source = mutableMapOf<Value, Value>(a to one)
              Map(source).also {
                source[a] = two
                source[b] = two
              }
            },
            "a list of pairs" to {
              val source = mutableListOf<Pair<Value, Value>>(a to one)
              Map(source).also {
                source[0] = a to two
                source += b to two
              }
            },
            "an array of pairs" to {
              val source = arrayOf<Pair<Value, Value>>(a to one)
              Map(*source).also { source[0] = a to two }
            },
          )
        ) { createAndChangeSource ->
          expect(createAndChangeSource().entryPairs()).toContain.inAnyOrder.only.values(a to one)
        }
      }

      describe("can’t be changed through the collections that its accessors return, even if they are cast to mutable types") {
        withIts(
          mapOf<String, (Map) -> Unit>(
            "clearing the keys" to { (it.keys as MutableSet<*>).clear() },
            "removing a key through an iterator" to { (it.keys.iterator() as MutableIterator<*>).apply { next() }.remove() },
            "clearing the values" to { (it.values as MutableCollection<*>).clear() },
            "removing a value through an iterator" to { (it.values.iterator() as MutableIterator<*>).apply { next() }.remove() },
            "clearing the entries" to { (it.entries as MutableSet<*>).clear() },
            "removing an entry through an iterator" to { (it.entries.iterator() as MutableIterator<*>).apply { next() }.remove() },
            "setting the value of an entry" to {
              @Suppress("UNCHECKED_CAST")
              (it.entries.first() as MutableMap.MutableEntry<Value, Value>).setValue(two)
            },
            "setting the value of an entry from an iterator" to {
              @Suppress("UNCHECKED_CAST")
              (it.entries.iterator().next() as MutableMap.MutableEntry<Value, Value>).setValue(two)
            },
          )
        ) { change ->
          val map = Map(a to one, b to half)
          val hashCode = map.hashCode()
          // Either the cast or the change may fail; what matters is that the map stays unchanged.
          runCatching { change(map) }
          expect(map) {
            feature(Map::entryPairs).toContain.inAnyOrder.only.values(a to one, b to half)
            feature(Map::hashCode).toEqual(hashCode)
          }
        }
      }

      it("keeps distinct symbols with the same identifier as distinct keys") {
        val otherA = Symbol("a")
        expect(Map(a to one, otherA to two).entryPairs()).toContain.inAnyOrder.only.values(a to one, otherA to two)
      }
    }

    describe("accessors") {
      describe("size") {
        withIts(examples) { (map, entries) ->
          expect(map.size).toEqual(entries.size)
        }
      }

      describe("isEmpty") {
        withIts(examples) { (map, entries) ->
          expect(map.isEmpty()).toEqual(entries.isEmpty())
        }
      }

      describe("keys") {
        withIts(examples) { (map, entries) ->
          expect(map.keys).toEqual(entries.map { it.first }.toSet())
        }
      }

      describe("values") {
        withIts(examples) { (map, entries) ->
          expect(map.values).toHaveSize(entries.size)
          for ((_, value) in entries) {
            expect(map.values).toContain(value)
          }
        }
      }

      describe("entries") {
        withIts(examples) { (map, entries) ->
          expect(map.entries).toHaveSize(entries.size)
          for (entry in entries) {
            expect(map.entryPairs()).toContain(entry)
          }
        }
      }

      describe("get") {
        describe("returns the value of every key") {
          withIts(examples) { (map, entries) ->
            for ((key, value) in entries) {
              expect(map[key]).toEqual(value)
            }
          }
        }

        describe("finds keys by equality") {
          withIts(presentKeyLookups) { (map, key, value) ->
            expect(map[key]).toEqual(value)
          }
        }

        describe("returns null for absent keys") {
          withIts(absentKeyLookups) { (map, key) ->
            expect(map[key]).toEqual(null)
          }
        }
      }

      describe("hasKey") {
        describe("recognises every key") {
          withIts(examples) { (map, entries) ->
            for ((key, _) in entries) {
              expect(map.hasKey(key)).toEqual(true)
            }
          }
        }

        describe("finds keys by equality") {
          withIts(presentKeyLookups) { (map, key) ->
            expect(map.hasKey(key)).toEqual(true)
          }
        }

        describe("rejects absent keys") {
          withIts(absentKeyLookups) { (map, key) ->
            expect(map.hasKey(key)).toEqual(false)
          }
        }
      }
    }

    // Maps are values, so equality must depend on their entries alone, like for Kotlin maps.
    describe("equals & hashCode") {
      describe("considers maps with equal entries equal and gives them the same hash code") {
        withIts(
          mapOf(
            "empty maps" to (Map() to Map()),
            "maps with the same entries" to (Map(a to one, b to two) to Map(a to one, b to two)),
            "maps with the same entries in a different order" to (Map(a to one, b to two) to Map(b to two, a to one)),
            "equal number keys from different sources" to (Map(Number(2) to a) to Map(Number(2.0) to a)),
            "equal number values from different sources" to (Map(a to half) to Map(a to Number(0.5))),
            "equal maps as keys" to (Map(Map(a to one) to b) to Map(Map(a to one) to b)),
            "equal maps as values" to (Map(a to Map(b to one)) to Map(a to Map(b to one))),
          )
        ) { (left, right) ->
          expect(left) {
            toEqual(right)
            feature(Map::hashCode).toEqual(right.hashCode())
          }
          expect(right) {
            toEqual(left)
            feature(Map::hashCode).toEqual(left.hashCode())
          }
        }
      }

      describe("considers maps with different entries unequal") {
        withIts(
          mapOf(
            "an empty and a non-empty map" to (Map() to Map(a to one)),
            "a map and a map with an additional entry" to (Map(a to one) to Map(a to one, b to two)),
            "different values for the same key" to (Map(a to one) to Map(a to two)),
            "different keys with the same value" to (Map(a to one) to Map(b to one)),
            "swapped keys and values" to (Map(one to two) to Map(two to one)),
            "distinct symbols with the same identifier as keys" to (Map(a to one) to Map(Symbol("a") to one)),
            "distinct symbols with the same identifier as values" to (Map(one to a) to Map(one to Symbol("a"))),
            "different maps as keys" to (Map(Map(a to one) to b) to Map(Map(a to two) to b)),
            "different maps as values" to (Map(a to Map(b to one)) to Map(a to Map(b to two))),
          )
        ) { (left, right) ->
          expect(left).notToEqual(right)
          expect(right).notToEqual(left)
        }
      }

      describe("considers maps unequal to other values") {
        withIts(
          mapOf(
            "an empty map and zero" to (Map() to Number(0)),
            "a map and a number" to (Map(one to one) to one),
            "a map and a symbol" to (Map(a to a) to a),
          )
        ) { (map, other) ->
          expect<Value>(map).notToEqual(other)
          expect(other).notToEqual(map)
        }
      }

      it("considers generated maps equal to their copies and gives them the same hash code") {
        checkAll(PropTestConfig(iterations = 200), Arb.value.map(maxNesting = 3)) { map ->
          val copy = Map(map.entryPairs().reversed())
          expect(map) {
            toEqual(map)
            toEqual(copy)
            feature(Map::hashCode).toEqual(copy.hashCode())
          }
          expect(copy).toEqual(map)
        }
      }
    }

    describe("toString") {
      describe("prints the entries in creation order, surrounded by braces") {
        withIts(
          mapOf(
            "an empty map" to (Map() to "{}"),
            "a single entry" to (Map(a to one) to "{a = 1}"),
            "several entries" to (Map(b to two, a to half, Number(-3) to b) to "{b = 2, a = ¹⁄₂, -3 = b}"),
            "nested maps" to (Map(a to Map(b to one), Map(one to two) to b) to "{a = {b = 1}, {1 = 2} = b}"),
          )
        ) { (map, expected) ->
          expect(map.toString()).toEqual(expected)
        }
      }

      it("prints one entry per line if the entries don’t fit on a single line") {
        val entries = (1..8).map { Number(it) to Symbol("symbol$it") }
        expect(Map(entries).toString()).toEqual(
          entries.joinToString(separator = ",\n", prefix = "{\n", postfix = "\n}") { (key, value) -> "\t$key = $value" }
        )
      }
    }
  }
})

private val a = Symbol("a")
private val b = Symbol("b")
private val one = Number(1)
private val two = Number(2)
private val half = Number(1) / Number(2)

private fun Map.entryPairs() = entries.map { it.key to it.value }

/** A map and the entries it was created from, which are all of its entries. */
private data class MapExample(val map: Map, val entries: List<Pair<Value, Value>>)

private val examples = mapOf(
  "an empty map" to MapExample(Map(), listOf()),
  "a single entry" to MapExample(Map(a to one), listOf(a to one)),
  "several entries" to MapExample(Map(a to one, b to half, two to a), listOf(a to one, b to half, two to a)),
  "several keys with the same value" to MapExample(Map(a to one, b to one), listOf(a to one, b to one)),
  "nested maps as keys and values" to
    MapExample(Map(Map(a to one) to Map(b to two)), listOf(Map(a to one) to Map(b to two))),
)

/** A map, a key that is equal to, but not the instance of, one of its keys, and that key’s value. */
private data class PresentKeyLookup(val map: Map, val key: Value, val value: Value)

private val presentKeyLookups = mapOf(
  "an integer created from a double" to PresentKeyLookup(Map(two to a), Number(2.0), a),
  "a fraction created from a double" to PresentKeyLookup(Map(half to a), Number(0.5), a),
  "an equal map" to PresentKeyLookup(Map(Map(a to one) to b), Map(a to one), b),
)

/** A map and a key that it doesn’t contain. */
private data class AbsentKeyLookup(val map: Map, val key: Value)

private val absentKeyLookups = mapOf(
  "any key in an empty map" to AbsentKeyLookup(Map(), a),
  "a different symbol" to AbsentKeyLookup(Map(a to one), b),
  "a distinct symbol with the same identifier" to AbsentKeyLookup(Map(a to one), Symbol("a")),
  "a different number" to AbsentKeyLookup(Map(one to a), two),
  "a value of the map" to AbsentKeyLookup(Map(a to one), one),
  "a map with fewer entries" to AbsentKeyLookup(Map(Map(a to one, b to two) to a), Map(a to one)),
  "the empty map" to AbsentKeyLookup(Map(Map(a to one) to a), Map()),
)
