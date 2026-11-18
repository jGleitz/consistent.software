package software.consistent.data.type.solvers.generic

import software.consistent.data.type.solvers.Cardinality.Companion.exactly
import software.consistent.data.type.solvers.Cardinality.Companion.moreThanIntMaxValue
import software.consistent.data.type.solvers.ClosedSolvableSet
import software.consistent.data.type.solvers.NormalizationOrdered
import software.consistent.data.type.solvers.doesNotSupportHashing
import software.consistent.data.type.solvers.normalizationOrder
import software.consistent.data.type.solvers.partition
import software.consistent.data.type.solvers.solverInvariant
import software.consistent.data.util.ComparisonCase
import software.consistent.data.util.compareEach
import software.consistent.data.value.Value
import software.consistent.data.value.monospaceLayout
import software.consistent.formatting.monospacelayout.Monospace
import software.consistent.formatting.monospacelayout.layouts.MonospaceLayout

internal interface EnumerationRepresented<
    TValue : Value,
    Self : EnumerationRepresented<TValue, Self>,
> : ClosedSolvableSet<TValue, Self> {
  val enumeration: EnumeratedValues<TValue, Self>
}

internal sealed class EnumeratedValues<
    TValue : Value,
    Rep : EnumerationRepresented<TValue, Rep>,
> : ClosedSolvableSet<TValue, Rep> {
  abstract val asRep: Rep

  protected abstract fun positive(members: NormalizationOrdered<TValue>): Rep

  protected abstract fun negative(members: NormalizationOrdered<TValue>): Rep

  abstract class Positive<TValue : Value, Rep : EnumerationRepresented<TValue, Rep>>(
      val orderedMembers: NormalizationOrdered<TValue>,
  ) : EnumeratedValues<TValue, Rep>() {
    init {
      checkValueUnionInvariant(orderedMembers)
    }

    override fun contains(value: TValue) = value in orderedMembers

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

    override fun with(otherSet: Rep): Rep =
        when (val other = otherSet.enumeration) {
          this -> asRep
          is Positive -> positive(orderedMembers + other.orderedMembers)
          is Negative -> negative(other.orderedMembers - orderedMembers)
        }

    override fun overlapWith(otherSet: Rep): Rep =
        when (val other = otherSet.enumeration) {
          this -> asRep
          is Positive -> positive(orderedMembers intersectedWith other.orderedMembers)
          is Negative -> positive(orderedMembers - other.orderedMembers)
        }

    override fun not(): Rep = negative(orderedMembers)

    // TODO implement directly
    override fun without(otherSet: Rep): Rep = this overlapWith !otherSet

    override fun isSubsetOf(largerSet: Rep) =
        when (val larger = largerSet.enumeration) {
          is Positive<*, *> -> larger.orderedMembers.containsAll(this.orderedMembers)
          is Negative<*, *> -> this.orderedMembers.none { larger.orderedMembers.contains(it) }
        }

    override fun triviallyEquals(otherSet: ClosedSolvableSet<*, *>) = this == otherSet

    override fun equals(other: Any?) =
        this === other || (other is Positive<*, *> && orderedMembers == other.orderedMembers)

    override fun hashCode(): Int = throw doesNotSupportHashing()

    override fun normalizationComparedTo(other: Rep) =
        when (val other = other.enumeration) {
          is Positive<TValue, Rep> ->
              compareEach(this.orderedMembers)
                  .toEach(other.orderedMembers, using = ::normalizationOrder)
                  .case

          is Negative<*, *> -> ComparisonCase.isLower
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

  abstract class Negative<TValue : Value, Rep : EnumerationRepresented<TValue, Rep>>(
      val orderedMembers: NormalizationOrdered<TValue>,
  ) : EnumeratedValues<TValue, Rep>() {
    /** Provides distinct members of the complete, infinite value domain. */
    protected abstract fun allMembers(): Sequence<TValue>

    protected abstract val completeLayout: MonospaceLayout

    init {
      checkValueUnionInvariant(orderedMembers)
    }

    override fun contains(value: TValue) = value !in orderedMembers

    override fun isEmpty() = false

    override fun isTriviallyEmpty() = false

    override fun isTriviallyNotEmpty() = true

    override fun isComplete() = orderedMembers.isEmpty()

    override fun isTriviallyComplete() = isComplete()

    override fun isFinite() = false

    override fun cardinality(upTo: Int) = moreThanIntMaxValue()

    override fun anyMember() = members.first()

    override val members
      get() = allMembers().filter { it !in orderedMembers }

    // [Base DNF
    // Operations](https://pnwamk.github.io/sst-tutorial/#%28part._.Base_.D.N.F_.Operations%29)

    override fun with(otherSet: Rep): Rep =
        when (val other = otherSet.enumeration) {
          this -> asRep
          is Positive -> negative(orderedMembers - other.orderedMembers)
          is Negative -> negative(orderedMembers intersectedWith other.orderedMembers)
        }

    override fun overlapWith(otherSet: Rep): Rep =
        when (val other = otherSet.enumeration) {
          this -> asRep
          is Positive -> positive(other.orderedMembers - orderedMembers)
          is Negative -> negative(orderedMembers + other.orderedMembers)
        }

    override fun not(): Rep = positive(orderedMembers)

    // TODO implement directly
    override fun without(otherSet: Rep): Rep = this overlapWith !otherSet

    override fun isSubsetOf(largerSet: Rep) =
        when (val larger = largerSet.enumeration) {
          is Positive<*, *> -> false
          is Negative<*, *> -> this.orderedMembers.containsAll(larger.orderedMembers)
        }

    override fun triviallyEquals(otherSet: ClosedSolvableSet<*, *>) = this == otherSet

    override fun equals(other: Any?) =
        this === other || (other is Negative<*, *> && orderedMembers == other.orderedMembers)

    override fun hashCode(): Int = throw doesNotSupportHashing()

    override fun normalizationComparedTo(other: Rep) =
        when (val other = other.enumeration) {
          is Positive<*, *> -> ComparisonCase.isHigher
          is Negative<TValue, Rep> ->
              compareEach(this.orderedMembers)
                  .toEach(other.orderedMembers, using = ::normalizationOrder)
                  .case
        }

    override val monospaceLayout
      get() =
          when (orderedMembers.size) {
            0 -> completeLayout
            1 -> orderedMembers.first().monospaceLayout.withPrefix("¬")
            else ->
                Monospace.line("∩") {
                  orderedMembers.map { it.monospaceLayout.withPrefix("¬") }
                }
          }

    override fun toString() = monospaceLayout.renderToString()
  }
}

private inline fun <reified Union : EnumeratedValues<*, *>> Union.checkValueUnionInvariant(
    members: Set<Value>,
) {
  solverInvariant {
    if (members.size >= 2) {
      val firstPartition = members.first().partition
      if (members.any { it.partition != firstPartition }) {
        rejectCreating<Union>()
            .with("members", members)
            .because("all members must be of the same partition")
      }
    }
  }
}
