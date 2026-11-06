package software.consistent.data.type.solvers.generic

import software.consistent.data.type.solvers.*
import software.consistent.data.type.solvers.Cardinality.Companion.exactly
import software.consistent.data.type.solvers.Cardinality.Companion.moreThanIntMaxValue
import software.consistent.data.util.ComparisonCase
import software.consistent.data.util.compareEach
import software.consistent.data.value.Value
import software.consistent.data.value.monospaceLayout
import software.consistent.formatting.monospacelayout.Monospace

internal sealed interface EnumeratedValues<TValue : Value> : ClosedSolvableSet<TValue, EnumeratedValues<TValue>> {
  private class Positive<TValue : Value>(val orderedMembers: NormalizationOrdered<TValue>) : EnumeratedValues<TValue> {
    init {
      checkValueUnionInvariant(orderedMembers)
    }

    override fun contains(value: TValue) = value in orderedMembers
    override fun anyMember() = orderedMembers.first()
    override val members get() = orderedMembers.asSequence()
    override fun isFinite() = true
    override fun isEmpty() = orderedMembers.isEmpty()
    override fun isTriviallyEmpty() = isEmpty()
    override fun isTriviallyNotEmpty() = orderedMembers.isNotEmpty()
    override fun isComplete() = false
    override fun isTriviallyComplete() = false

    override fun cardinality(upTo: Int) = exactly(orderedMembers.size)

    // [Base DNF Operations](https://pnwamk.github.io/sst-tutorial/#%28part._.Base_.D.N.F_.Operations%29)

    override fun with(otherSet: EnumeratedValues<TValue>) = when (otherSet) {
      this -> this
      is Positive -> Positive(orderedMembers + otherSet.orderedMembers)
      is Negative -> Negative(otherSet.orderedMembers - orderedMembers)
    }

    override fun overlapWith(otherSet: EnumeratedValues<TValue>) = when (otherSet) {
      this -> this
      is Positive -> Positive(orderedMembers intersectedWith otherSet.orderedMembers)
      is Negative -> Positive(orderedMembers - otherSet.orderedMembers)
    }

    override fun not() = Negative(orderedMembers)

    // TODO implement directly
    override fun without(otherSet: EnumeratedValues<TValue>) = this overlapWith !otherSet

    override fun isSubsetOf(largerSet: EnumeratedValues<TValue>) = when (largerSet) {
      is Positive<*> -> largerSet.orderedMembers.containsAll(this.orderedMembers)
      is Negative<*> -> this.orderedMembers.none { largerSet.orderedMembers.contains(it) }
    }

    override fun triviallyEquals(otherSet: ClosedSolvableSet<*, *>) = this == otherSet

    override fun equals(other: Any?) = this === other || (other is Positive<*> && orderedMembers == other.orderedMembers)

    override fun hashCode(): Int = throw doesNotSupportHashing()

    override fun normalizationComparedTo(other: EnumeratedValues<TValue>) = when (other) {
      is Positive<TValue> -> compareEach(this.orderedMembers).toEach(other.orderedMembers, using = ::normalizationOrder).case

      is Negative<*> -> ComparisonCase.isLower
    }

    override val monospaceLayout
      get() = when (orderedMembers.size) {
        0 -> Monospace.atom("∅")
        1 -> orderedMembers.first().monospaceLayout
        else -> Monospace.line("∪") {
          orderedMembers.map { it.monospaceLayout }
        }
      }

    override fun toString() = monospaceLayout.renderToString()
  }

  private class Negative<TValue : Value>(val orderedMembers: NormalizationOrdered<TValue>) : EnumeratedValues<TValue> {
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
    override fun anyMember() = TODO("Not yet implemented")

    override val members get() = throw mustNotRequestMembersOfInfiniteType()

    // [Base DNF Operations](https://pnwamk.github.io/sst-tutorial/#%28part._.Base_.D.N.F_.Operations%29)

    override fun with(otherSet: EnumeratedValues<TValue>) = when (otherSet) {
      this -> this
      is Positive -> Negative(orderedMembers - otherSet.orderedMembers)
      is Negative -> Negative(orderedMembers intersectedWith otherSet.orderedMembers)
    }

    override fun overlapWith(otherSet: EnumeratedValues<TValue>) = when (otherSet) {
      this -> this
      is Positive -> Positive(otherSet.orderedMembers - orderedMembers)
      is Negative -> Negative(orderedMembers + otherSet.orderedMembers)
    }

    override fun not() = Positive(orderedMembers)

    // TODO implement directly
    override fun without(otherSet: EnumeratedValues<TValue>) = this overlapWith !otherSet

    override fun isSubsetOf(largerSet: EnumeratedValues<TValue>) = when (largerSet) {
      is Positive<*> -> false
      is Negative<*> -> this.orderedMembers.containsAll(largerSet.orderedMembers)
    }

    override fun triviallyEquals(otherSet: ClosedSolvableSet<*, *>) = this == otherSet

    override fun equals(other: Any?) = this === other || (other is Negative<*> && orderedMembers == other.orderedMembers)

    override fun hashCode(): Int = throw doesNotSupportHashing()

    override fun normalizationComparedTo(other: EnumeratedValues<TValue>) = when (other) {
      is Positive<*> -> ComparisonCase.isHigher
      is Negative<TValue> -> compareEach(this.orderedMembers).toEach(other.orderedMembers, using = ::normalizationOrder).case
    }

    override val monospaceLayout
      get() = when (orderedMembers.size) {
        0 -> Monospace.atom("AnySymbol") // TODO this class is supposed to be agnostic! We probably need a subclass.
        1 -> orderedMembers.first().monospaceLayout.withPrefix("¬")
        else -> Monospace.line("∩") {
          orderedMembers.map { it.monospaceLayout.withPrefix("¬") }
        }
      }

    override fun toString() = monospaceLayout.renderToString()
  }

  companion object {
    private val empty = Positive<Nothing>(NormalizationOrdered.empty())
    private val complete = Negative<Nothing>(NormalizationOrdered.empty())

    @Suppress("UNCHECKED_CAST")
    internal fun <TValue : Value> empty(): EnumeratedValues<TValue> = empty as EnumeratedValues<TValue>

    @Suppress("UNCHECKED_CAST")
    internal fun <TValue : Value> complete(): EnumeratedValues<TValue> = complete as EnumeratedValues<TValue>

    internal fun <TValue : Value> of(vararg values: TValue): EnumeratedValues<TValue> = Positive(NormalizationOrdered.of(*values))
  }
}

private inline fun <reified Union : EnumeratedValues<out Value>> Union.checkValueUnionInvariant(members: Set<Value>) {
  solverInvariant {
    if (members.size >= 2) {
      val firstPartition = members.first().partition
      if (members.any { it.partition != firstPartition }) {
        rejectCreating<Union>().with("members", members).because("all members must be of the same partition")
      }
    }
  }
}
