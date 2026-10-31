package software.consistent.data.value

import ch.tutteli.atrium.api.fluent.en_GB.*
import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withIts
import kotlin.math.sign

class SymbolSpec :
  DescribeSpec({
    describe("Symbol") {
      // Symbols have referential equality: every created symbol is a value of its own, whatever its identifier.
      describe("equals & hashCode") {
        it("considers a symbol equal to itself and gives it the same hash code every time") {
          val symbol = Symbol("a")
          expect(symbol) {
            toEqual(symbol)
            feature(Symbol::hashCode).toEqual(symbol.hashCode())
          }
        }

        describe("considers distinct symbols unequal, even if their identifiers are equal") {
          withIts(
            mapOf(
              "equal identifiers" to (Symbol("a") to Symbol("a")),
              "different identifiers" to (Symbol("a") to Symbol("b")),
              "TRUE and a created symbol with its identifier" to (TRUE to Symbol("true")),
              "FALSE and a created symbol with its identifier" to (FALSE to Symbol("false")),
              "TRUE and FALSE" to (TRUE to FALSE),
            ),
          ) { (left, right) ->
            expect(left).notToEqual(right)
            expect(right).notToEqual(left)
          }
        }

        it("keeps distinct symbols with equal identifiers apart in hash-based collections") {
          val first = Symbol("a")
          val second = Symbol("a")
          val set = hashSetOf<Value>(first, second, first)
          expect(set).toHaveSize(2)
        }

        describe("considers symbols unequal to other values") {
          withIts(
            mapOf(
              "a symbol and the number its identifier spells" to (Symbol("1") to Number(1)),
              "a symbol and the empty map its identifier spells" to (Symbol("{}") to Map()),
            ),
          ) { (symbol, other) ->
            expect<Value>(symbol).notToEqual(other)
            expect(other).notToEqual(symbol)
          }
        }
      }

      describe("toString") {
        describe("prints the identifier") {
          withIts(
            mapOf(
              "a single letter" to (Symbol("a") to "a"),
              "a name with underscores" to (Symbol("Harry_Potter") to "Harry_Potter"),
              "a name with non-ASCII letters" to (Symbol("Größe") to "Größe"),
              "TRUE" to (TRUE to "true"),
              "FALSE" to (FALSE to "false"),
            ),
          ) { (symbol, expected) ->
            expect(symbol.toString()).toEqual(expected)
          }
        }
      }

      describe("compareIdentifiersTo") {
        describe("orders symbols by their identifiers, regardless of whether they are equal") {
          withIts(
            mapOf(
              "the same symbol" to Triple(TRUE, TRUE, 0),
              "distinct symbols with equal identifiers" to Triple(Symbol("a"), Symbol("a"), 0),
              "a smaller identifier" to Triple(Symbol("a"), Symbol("b"), -1),
              "a greater identifier" to Triple(Symbol("b"), Symbol("a"), 1),
              "a prefix of the other identifier" to Triple(Symbol("a"), Symbol("ab"), -1),
              "an extension of the other identifier" to Triple(Symbol("ab"), Symbol("a"), 1),
            ),
          ) { (left, right, expectedSign) ->
            expect((left compareIdentifiersTo right).sign).toEqual(expectedSign)
          }
        }
      }
    }
  })
