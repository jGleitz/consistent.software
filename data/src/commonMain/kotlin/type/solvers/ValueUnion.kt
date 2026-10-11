package software.consistent.data.type.solvers

import software.consistent.data.formatting.monospacelayout.Monospace
import software.consistent.data.util.ComparisonCase
import software.consistent.data.util.compareEach
import software.consistent.data.value.Value
import software.consistent.data.value.monospaceLayout

internal sealed interface ValueUnion<TValue : Value> : ClosedSolvableSet<TValue, ValueUnion<TValue>> {
  private class Positive<TValue : Value>(val orderedMembers: NormalizationOrdered<TValue>) : ValueUnion<TValue> {
    init {
      checkValueUnionInvariant(orderedMembers)
    }

    override fun contains(value: TValue) = value in orderedMembers
    override fun anyMember() = orderedMembers.first()
    override val members get() = orderedMembers.asSequence()
    override fun isFinite() = true
    override fun isTriviallyEmpty() = isEmpty()
    override fun isEmpty() = orderedMembers.isEmpty()
    override fun isTriviallyComplete() = false
    override fun isComplete() = false

    override fun cardinality(max: Int) = orderedMembers.size

    // [Base DNF Operations](https://pnwamk.github.io/sst-tutorial/#%28part._.Base_.D.N.F_.Operations%29)

    override fun with(otherSet: ValueUnion<TValue>) = when (otherSet) {
      this -> this
      is Positive -> Positive(orderedMembers + otherSet.orderedMembers)
      is Negative -> Negative(otherSet.orderedMembers - orderedMembers)
    }

    override fun overlapWith(otherSet: ValueUnion<TValue>) = when (otherSet) {
      this -> this
      is Positive -> Positive(orderedMembers intersectedWith otherSet.orderedMembers)
      is Negative -> Positive(orderedMembers - otherSet.orderedMembers)
    }

    override fun not() = Negative(orderedMembers)

    // TODO implement directly
    override fun without(otherSet: ValueUnion<TValue>) = this overlapWith !otherSet

    override fun isSubsetOf(largerSet: ValueUnion<TValue>) = when (largerSet) {
      is Positive<*> -> largerSet.orderedMembers.containsAll(this.orderedMembers)
      is Negative<*> -> this.orderedMembers.none { largerSet.orderedMembers.contains(it) }
    }

    override fun normalizationComparedTo(other: ValueUnion<TValue>) = when (other) {
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

  private class Negative<TValue : Value>(val orderedMembers: NormalizationOrdered<TValue>) : ValueUnion<TValue> {
    init {
      checkValueUnionInvariant(orderedMembers)
    }

    override fun contains(value: TValue) = value !in orderedMembers
    override fun isTriviallyEmpty() = false
    override fun isEmpty() = false
    override fun isTriviallyComplete() = isComplete()
    override fun isComplete() = orderedMembers.isEmpty()
    override fun isFinite() = false
    override fun cardinality(max: Int) = throw mustNotRequestCardinalityOfInfiniteType()
    override fun anyMember() = TODO("Not yet implemented")

    override val members get() = throw mustNotRequestMembersOfInfiniteType()

    // [Base DNF Operations](https://pnwamk.github.io/sst-tutorial/#%28part._.Base_.D.N.F_.Operations%29)

    override fun with(otherSet: ValueUnion<TValue>) = when (otherSet) {
      this -> this
      is Positive -> Negative(orderedMembers - otherSet.orderedMembers)
      is Negative -> Negative(orderedMembers intersectedWith otherSet.orderedMembers)
    }

    override fun overlapWith(otherSet: ValueUnion<TValue>) = when (otherSet) {
      this -> this
      is Positive -> Positive(otherSet.orderedMembers - orderedMembers)
      is Negative -> Negative(orderedMembers + otherSet.orderedMembers)
    }

    override fun not() = Positive(orderedMembers)

    // TODO implement directly
    override fun without(otherSet: ValueUnion<TValue>) = this overlapWith !otherSet

    override fun isSubsetOf(largerSet: ValueUnion<TValue>) = when (largerSet) {
      is Positive<*> -> false
      is Negative<*> -> this.orderedMembers.containsAll(largerSet.orderedMembers)
    }

    override fun normalizationComparedTo(other: ValueUnion<TValue>) = when (other) {
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
    internal fun <TValue : Value> empty(): ValueUnion<TValue> = empty as ValueUnion<TValue>

    @Suppress("UNCHECKED_CAST")
    internal fun <TValue : Value> complete(): ValueUnion<TValue> = complete as ValueUnion<TValue>

    internal fun <TValue : Value> of(vararg values: TValue): ValueUnion<TValue> = Positive(NormalizationOrdered.of(*values))
  }
}

private inline fun <reified Union : ValueUnion<out Value>> Union.checkValueUnionInvariant(members: Set<Value>) {
  solverInvariant {
    if (members.size >= 2) {
      val firstPartition = members.first().partition
      if (members.any { it.partition != firstPartition }) {
        rejectCreating<Union>().with("members", members).because("all members must be of the same partition")
      }
    }
  }
}
