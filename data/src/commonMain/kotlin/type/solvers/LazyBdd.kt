package software.consistent.data.type.solvers

import software.consistent.data.type.solvers.ClosedSolvableSet.Cardinality.Companion.exactly
import software.consistent.data.type.solvers.ClosedSolvableSet.Cardinality.Companion.moreThanIntMaxValue
import software.consistent.data.util.ComparisonCase
import software.consistent.data.util.ComparisonCase.*

internal sealed interface LazyBdd<TValue, Atom : ValueSet.NormalizationComparable<TValue, Atom>, Self : LazyBdd<TValue, Atom, Self>> :
  ClosedSolvableSet<TValue, Self> {
  abstract class Node<TValue, Atom : ValueSet.NormalizationComparable<TValue, Atom>, Bdd : LazyBdd<TValue, Atom, Bdd>>(
    val discriminator: Atom,
    val ifTrue: Bdd,
    val regardless: Bdd,
    val ifFalse: Bdd,
  ) : LazyBdd<TValue, Atom, Bdd> {
    protected abstract fun node(
      discriminator: Atom,
      ifTrue: Bdd,
      regardless: Bdd,
      ifFalse: Bdd,
    ): Bdd

    protected abstract fun top(): Bdd

    protected abstract fun bottom(): Bdd

    protected fun bdd(
      discriminator: Atom,
      ifTrue: Bdd,
      regardless: Bdd,
      ifFalse: Bdd,
    ): Bdd =
      when {
        regardless is Top<*, *, *> -> regardless
        ifTrue == ifFalse -> ifTrue with regardless
        else -> node(discriminator, ifTrue, regardless, ifFalse)
      }

    override fun contains(value: TValue) = (if (value in discriminator) value in ifTrue else value in ifFalse) || value in regardless

    // https://pnwamk.github.io/sst-tutorial/#%28counter._%28figure._fig~3abdd-or%29%29
    override fun with(otherSet: Bdd): Bdd =
      @Suppress("UNCHECKED_CAST")
      when (otherSet) {
        this -> {
          otherSet
        }

        is Top<*, *, *> -> {
          otherSet
        }

        is Bottom<*, *, *> -> {
          this as Bdd
        }

        is Node<TValue, Atom, Bdd> -> {
          when (discriminator normalizationComparedTo otherSet.discriminator) {
            isLower -> {
              bdd(discriminator, ifTrue, regardless with otherSet, ifFalse)
            }

            isEqual -> {
              bdd(
                discriminator,
                ifTrue with otherSet.ifTrue,
                regardless with otherSet.regardless,
                ifFalse with otherSet.ifFalse,
              )
            }

            isHigher -> {
              bdd(otherSet.discriminator, otherSet.ifTrue, otherSet.regardless with (this as Bdd), otherSet.ifFalse)
            }
          }
        }
      }

    // https://pnwamk.github.io/sst-tutorial/#%28counter._%28figure._fig~3abdd-and%29%29
    @Suppress("UNCHECKED_CAST")
    override fun overlapWith(otherSet: Bdd): Bdd =
      when (otherSet) {
        this -> {
          this as Bdd
        }

        is Top<*, *, *> -> {
          this as Bdd
        }

        is Bottom<*, *, *> -> {
          otherSet
        }

        is Node<TValue, Atom, Bdd> -> {
          when (discriminator normalizationComparedTo otherSet.discriminator) {
            isLower -> {
              bdd(
                discriminator,
                ifTrue overlapWith otherSet,
                regardless overlapWith otherSet,
                ifFalse overlapWith otherSet,
              )
            }

            isEqual -> {
              bdd(
                discriminator,
                (ifTrue with regardless) overlapWith (otherSet.ifTrue with otherSet.regardless),
                bottom(),
                (ifFalse with regardless) overlapWith (otherSet.ifFalse with otherSet.regardless),
              )
            }

            isHigher -> {
              bdd(
                otherSet.discriminator,
                otherSet.ifTrue overlapWith (this as Bdd),
                otherSet.regardless overlapWith (this as Bdd),
                otherSet.ifFalse overlapWith (this as Bdd),
              )
            }
          }
        }
      }

    // https://pnwamk.github.io/sst-tutorial/#%28counter._%28figure._fig~3abdd-diff%29%29
    // also spells out set difference explicitly. However, we currently reduce it to `a intersectedWith !b`

    // https://pnwamk.github.io/sst-tutorial/#%28counter._%28figure._fig~3abdd-not%29%29
    // TODO I _think_ we can just directly call node() instead of bdd() at least in some of the cases
    override fun not() =
      when {
        ifFalse is Bottom<*, *, *> -> bdd(discriminator, bottom(), !(ifTrue with regardless), !regardless)
        ifTrue is Bottom<*, *, *> -> bdd(discriminator, !regardless, !(ifFalse with regardless), bottom())
        regardless is Bottom<*, *, *> -> bdd(discriminator, !ifTrue, !(ifTrue with ifFalse), !ifFalse)
        else -> bdd(discriminator, !(ifTrue with regardless), bottom(), !(ifFalse with regardless))
      }

    override fun normalizationComparedTo(other: Bdd): ComparisonCase =
      when (other) {
        is Bottom<*, *, *> -> {
          isHigher
        }

        is Top<*, *, *> -> {
          isLower
        }

        is Node<TValue, Atom, Bdd> -> {
          (this.discriminator normalizationComparedTo other.discriminator)
            .ifEqual { this.ifTrue normalizationComparedTo other.ifTrue }
            .ifEqual { this.regardless normalizationComparedTo other.regardless }
            .ifEqual { this.ifFalse normalizationComparedTo other.ifFalse }
        }
      }
  }

  abstract class Top<TValue, Atom : ValueSet.NormalizationComparable<TValue, Atom>, Bdd : LazyBdd<TValue, Atom, Bdd>> :
    LazyBdd<TValue, Atom, Bdd> {
    override fun contains(value: TValue) = true

    override fun isTriviallyEmpty() = false

    override fun isEmpty() = false

    override fun isTriviallyComplete() = true

    override fun isComplete() = true

    override fun isFinite() = false

    override fun cardinality(upTo: Int) = moreThanIntMaxValue()

    override val members: Sequence<TValue> get() = throw mustNotRequestMembersOfInfiniteType()

    @Suppress("UNCHECKED_CAST")
    override fun with(otherSet: Bdd) = this as Bdd

    override fun overlapWith(otherSet: Bdd) = otherSet

    override fun without(otherSet: Bdd) = !otherSet

    override fun isSubsetOf(largerSet: Bdd) = largerSet.isComplete()

    override fun normalizationComparedTo(other: Bdd) =
      when (other) {
        is Top<*, *, *> -> isEqual
        is Bottom<*, *, *>, is Node<*, *, *> -> isHigher
      }
  }

  abstract class Bottom<TValue, Atom : ValueSet.NormalizationComparable<TValue, Atom>, Bdd : LazyBdd<TValue, Atom, Bdd>> :
    LazyBdd<TValue, Atom, Bdd> {
    override fun contains(value: TValue) = false

    override fun isTriviallyEmpty() = true

    override fun isEmpty() = true

    override fun isTriviallyComplete() = false

    override fun isComplete() = false

    override fun isFinite() = true

    override fun cardinality(upTo: Int) = exactly(0)

    override fun anyMember() = null

    override val members get() = emptySequence<TValue>()

    override fun with(otherSet: Bdd) = otherSet

    @Suppress("UNCHECKED_CAST")
    override fun overlapWith(otherSet: Bdd) = this as Bdd

    @Suppress("UNCHECKED_CAST")
    override fun without(otherSet: Bdd) = this as Bdd

    override fun isSubsetOf(largerSet: Bdd) = true

    override fun normalizationComparedTo(other: Bdd) =
      when (other) {
        is Bottom<*, *, *> -> isEqual
        is Top<*, *, *>, is Node<*, *, *> -> isLower
      }
  }
}
