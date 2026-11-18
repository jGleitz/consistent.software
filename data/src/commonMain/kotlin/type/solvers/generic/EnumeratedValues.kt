package software.consistent.data.type.solvers.generic

import software.consistent.data.type.solvers.Cardinality.Companion.exactly
import software.consistent.data.type.solvers.Cardinality.Companion.moreThanIntMaxValue
import software.consistent.data.type.solvers.ClosedSolvableSet
import software.consistent.data.type.solvers.NormalizationOrdered
import software.consistent.data.type.solvers.doesNotSupportHashing
import software.consistent.data.type.solvers.normalizationOrder
import software.consistent.data.type.solvers.symbolMembers
import software.consistent.data.util.ComparisonCase
import software.consistent.data.util.compareEach
import software.consistent.data.value.Value
import software.consistent.data.value.monospaceLayout
import software.consistent.formatting.monospacelayout.Monospace

internal sealed interface EnumeratedValues : ClosedSolvableSet<Value.Symbol, EnumeratedValues> {
  private class Positive(val orderedMembers: NormalizationOrdered<Value.Symbol>) :
      EnumeratedValues {
    override fun contains(value: Value.Symbol) = value in orderedMembers

    override fun anyMember() = orderedMembers.firstOrNull()

    override val members
      get() = orderedMembers.asSequence()

    override fun isFinite() = true

    override fun isEmpty() = orderedMembers.isEmpty()

    override fun isTriviallyEmpty() = isEmpty()

    override fun isTriviallyNotEmpty() = orderedMembers.isNotEmpty()

    override fun isComplete() = false

    override fun isTriviallyComplete() = false

    override fun cardinality(upTo: Int) = exactly(orderedMembers.size)

    // [Base DNF
    // Operations](https://pnwamk.github.io/sst-tutorial/#%28part._.Base_.D.N.F_.Operations%29)

    override fun with(otherSet: EnumeratedValues) =
        when (otherSet) {
          this -> this
          is Positive -> Positive(orderedMembers + otherSet.orderedMembers)
          is Negative -> Negative(otherSet.orderedMembers - orderedMembers)
        }

    override fun overlapWith(otherSet: EnumeratedValues) =
        when (otherSet) {
          this -> this
          is Positive -> Positive(orderedMembers intersectedWith otherSet.orderedMembers)
          is Negative -> Positive(orderedMembers - otherSet.orderedMembers)
        }

    override fun not() = Negative(orderedMembers)

    // TODO implement directly
    override fun without(otherSet: EnumeratedValues) = this overlapWith !otherSet

    override fun isSubsetOf(largerSet: EnumeratedValues) =
        when (largerSet) {
          is Positive -> largerSet.orderedMembers.containsAll(this.orderedMembers)
          is Negative -> this.orderedMembers.none { largerSet.orderedMembers.contains(it) }
        }

    override fun triviallyEquals(otherSet: ClosedSolvableSet<*, *>) = this == otherSet

    override fun equals(other: Any?) =
        this === other || (other is Positive && orderedMembers == other.orderedMembers)

    override fun hashCode(): Int = throw doesNotSupportHashing()

    override fun normalizationComparedTo(other: EnumeratedValues) =
        when (other) {
          is Positive ->
              compareEach(this.orderedMembers)
                  .toEach(other.orderedMembers, using = ::normalizationOrder)
                  .case

          is Negative -> ComparisonCase.isLower
        }

    override val monospaceLayout
      get() =
          when (orderedMembers.size) {
            0 -> Monospace.atom("∅")
            1 -> orderedMembers.first().monospaceLayout
            else ->
                Monospace.line("∪") {
                  orderedMembers.map { it.monospaceLayout }
                }
          }

    override fun toString() = monospaceLayout.renderToString()
  }

  private class Negative(val orderedMembers: NormalizationOrdered<Value.Symbol>) :
      EnumeratedValues {
    override fun contains(value: Value.Symbol) = value !in orderedMembers

    override fun isEmpty() = false

    override fun isTriviallyEmpty() = false

    override fun isTriviallyNotEmpty() = true

    override fun isComplete() = orderedMembers.isEmpty()

    override fun isTriviallyComplete() = isComplete()

    override fun isFinite() = false

    override fun cardinality(upTo: Int) = moreThanIntMaxValue()

    override fun anyMember() = members.first()

    override val members
      get() = symbolMembers().filter { it !in orderedMembers }

    // [Base DNF
    // Operations](https://pnwamk.github.io/sst-tutorial/#%28part._.Base_.D.N.F_.Operations%29)

    override fun with(otherSet: EnumeratedValues) =
        when (otherSet) {
          this -> this
          is Positive -> Negative(orderedMembers - otherSet.orderedMembers)
          is Negative -> Negative(orderedMembers intersectedWith otherSet.orderedMembers)
        }

    override fun overlapWith(otherSet: EnumeratedValues) =
        when (otherSet) {
          this -> this
          is Positive -> Positive(otherSet.orderedMembers - orderedMembers)
          is Negative -> Negative(orderedMembers + otherSet.orderedMembers)
        }

    override fun not() = Positive(orderedMembers)

    // TODO implement directly
    override fun without(otherSet: EnumeratedValues) = this overlapWith !otherSet

    override fun isSubsetOf(largerSet: EnumeratedValues) =
        when (largerSet) {
          is Positive -> false
          is Negative -> this.orderedMembers.containsAll(largerSet.orderedMembers)
        }

    override fun triviallyEquals(otherSet: ClosedSolvableSet<*, *>) = this == otherSet

    override fun equals(other: Any?) =
        this === other || (other is Negative && orderedMembers == other.orderedMembers)

    override fun hashCode(): Int = throw doesNotSupportHashing()

    override fun normalizationComparedTo(other: EnumeratedValues) =
        when (other) {
          is Positive -> ComparisonCase.isHigher
          is Negative ->
              compareEach(this.orderedMembers)
                  .toEach(other.orderedMembers, using = ::normalizationOrder)
                  .case
        }

    override val monospaceLayout
      get() =
          when (orderedMembers.size) {
            0 ->
                Monospace.atom(
                    "AnySymbol",
                )
            1 -> orderedMembers.first().monospaceLayout.withPrefix("¬")
            else ->
                Monospace.line("∩") {
                  orderedMembers.map { it.monospaceLayout.withPrefix("¬") }
                }
          }

    override fun toString() = monospaceLayout.renderToString()
  }

  companion object {
    private val empty = Positive(NormalizationOrdered.empty())
    private val complete = Negative(NormalizationOrdered.empty())

    internal fun empty(): EnumeratedValues = empty

    internal fun complete(): EnumeratedValues = complete

    internal fun of(vararg values: Value.Symbol): EnumeratedValues =
        Positive(NormalizationOrdered.of(*values))
  }
}
