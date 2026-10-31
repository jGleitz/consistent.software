package software.consistent.data.type.solvers

import software.consistent.data.formatting.monospacelayout.Monospace
import software.consistent.data.formatting.monospacelayout.WithMonospaceLayout
import software.consistent.data.util.ComparisonCase
import software.consistent.data.value.Value

internal data class PartitionedTypeUnion(
  private val symbols: ValueUnion<Value.Symbol>,
  private val numbers: DisjunctIntervalUnion<Value.Number>,
  private val maps: MapSolver,
) : SolvableSet,
  WithMonospaceLayout {
  override fun contains(value: Value) =
    when (value) {
      is Value.Symbol -> value in symbols
      is Value.Number -> value in numbers
      is Value.Map -> value in maps
    }

  override fun isTriviallyEmpty() = symbols.isTriviallyEmpty() && numbers.isTriviallyEmpty() && maps.isTriviallyEmpty()

  override fun isEmpty() = symbols.isEmpty() && numbers.isEmpty() && maps.isEmpty()

  override fun isTriviallyComplete() = symbols.isTriviallyComplete() && numbers.isTriviallyComplete() && maps.isTriviallyComplete()

  override fun isComplete() = symbols.isComplete() && numbers.isComplete() && maps.isComplete()

  override fun isFinite() = symbols.isFinite() && numbers.isFinite() && maps.isFinite()

  override fun cardinality(upTo: Int): ClosedSolvableSet.Cardinality {
    var result = symbols.cardinality(upTo)
    if (result > upTo) return result

    result += numbers.cardinality(upTo - result.lowerBound)
    if (result > upTo) return result

    result += maps.cardinality(upTo - result.lowerBound)
    return result
  }

  override fun anyMember() = symbols.anyMember() ?: maps.anyMember() // TODO numbers

  override val members get() = symbols.members + numbers.members + maps.members

  // https://pnwamk.github.io/sst-tutorial/#%28part._.Type_.Operations%29
  override fun with(otherSet: SolvableSet): SolvableSet = this with otherSet.asPartitionedTypeUnion()

  infix fun with(otherSet: PartitionedTypeUnion) =
    PartitionedTypeUnion(
      this.symbols with otherSet.symbols,
      this.numbers with otherSet.numbers,
      this.maps with otherSet.maps,
    )

  override fun overlapWith(otherSet: SolvableSet): SolvableSet = this overlapWith otherSet.asPartitionedTypeUnion()

  infix fun overlapWith(otherSet: PartitionedTypeUnion) =
    PartitionedTypeUnion(
      this.symbols overlapWith otherSet.symbols,
      this.numbers overlapWith otherSet.numbers,
      this.maps overlapWith otherSet.maps,
    ) // TODO

  override fun not() = PartitionedTypeUnion(!symbols, !numbers, !maps)

  override fun without(otherSet: SolvableSet) = this without otherSet.asPartitionedTypeUnion()

  infix fun without(otherSet: PartitionedTypeUnion) =
    PartitionedTypeUnion(
      this.symbols without otherSet.symbols,
      this.numbers without otherSet.numbers,
      this.maps without otherSet.maps,
    )

  override fun isSubsetOf(largerSet: SolvableSet) = this isSubsetOf largerSet.asPartitionedTypeUnion()

  infix fun isSubsetOf(largerSet: PartitionedTypeUnion) =
    this.symbols isSubsetOf largerSet.symbols && this.numbers isSubsetOf largerSet.numbers && this.maps isSubsetOf largerSet.maps

  override fun normalizationComparedTo(other: SolvableSet) = this normalizationComparedTo other.asPartitionedTypeUnion()

  infix fun normalizationComparedTo(other: PartitionedTypeUnion): ComparisonCase =
    (this.symbols normalizationComparedTo other.symbols)
      .ifEqual { this.numbers normalizationComparedTo other.numbers }
      .ifEqual { this.maps normalizationComparedTo other.maps }

  override val monospaceLayout
    get() =
      when {
        isTriviallyEmpty() -> {
          Monospace.atom("∅")
        }

        isTriviallyComplete() -> {
          Monospace.atom("Any")
        }

        else -> {
          Monospace.line("∪", 3) {
            if (!symbols.isTriviallyEmpty()) {
              -symbols.monospaceLayout.containerWrappedIn("(", ")")
            }
            if (!numbers.isTriviallyEmpty()) {
              -numbers.monospaceLayout.containerWrappedIn("(", ")")
            }
            if (!maps.isTriviallyEmpty()) {
              -maps.monospaceLayout.containerWrappedIn("(", ")")
            }
          }
        }
      }

  override fun toString() = monospaceLayout.renderToString()

  internal companion object {
    private val complete = PartitionedTypeUnion(ValueUnion.complete(), DisjunctIntervalUnion.complete(), MapSolver.complete())
    private val empty = PartitionedTypeUnion(ValueUnion.empty(), DisjunctIntervalUnion.empty(), MapSolver.empty())

    internal fun complete() = complete

    internal fun empty() = empty

    private fun SolvableSet.asPartitionedTypeUnion() =
      when (this) {
        is PartitionedTypeUnion -> this
      }
  }
}
