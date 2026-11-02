package software.consistent.data.type.solvers

import software.consistent.data.type.solvers.Cardinality.Companion.exactly
import software.consistent.data.type.solvers.Cardinality.Companion.moreThanIntMaxValue
import software.consistent.data.util.ComparisonCase.*

internal interface BddRepresented<TValue, Atom : ClosedSolvableSet<TValue, in Atom>, Self : BddRepresented<TValue, Atom, Self>>
  : ClosedSolvableSet<TValue, Self> {
  val bdd: LazyBdd<TValue, Atom, Self>
}

internal sealed interface LazyBdd<TValue, Atom : ClosedSolvableSet<TValue, in Atom>, Rep : BddRepresented<TValue, Atom, Rep>> :
  ClosedSolvableSet<TValue, Rep> {
  val asRep: Rep

  abstract class DecisionNode<TValue, Atom : ClosedSolvableSet<TValue, in Atom>, Rep : BddRepresented<TValue, Atom, Rep>> :
    LazyBdd<TValue, Atom, Rep> {
    abstract val discriminator: Atom
    abstract val ifTrue: Rep
    abstract val regardless: Rep
    abstract val ifFalse: Rep

    protected abstract fun top(): Rep

    protected abstract fun bottom(): Rep

    protected abstract fun node(
      discriminator: Atom,
      ifTrue: Rep,
      regardless: Rep,
      ifFalse: Rep
    ): Rep

    override fun contains(value: TValue) =
      (if (value in discriminator) value in ifTrue else value in ifFalse) || value in regardless

    // https://pnwamk.github.io/sst-tutorial/#%28counter._%28figure._fig~3abdd-or%29%29
    override fun with(otherSet: Rep): Rep = ifNonTrivialWith(otherSet) { otherDecisionNode ->
      when (discriminator normalizationComparedTo otherDecisionNode.discriminator) {
        isLower -> node(discriminator, ifTrue, regardless with otherSet, ifFalse)
        isEqual -> node(
          discriminator,
          ifTrue with otherDecisionNode.ifTrue,
          regardless with otherDecisionNode.regardless,
          ifFalse with otherDecisionNode.ifFalse
        )

        isHigher -> node(
          otherDecisionNode.discriminator,
          otherDecisionNode.ifTrue,
          otherDecisionNode.regardless with this.asRep,
          otherDecisionNode.ifFalse
        )
      }
    }

    private inline fun ifNonTrivialWith(otherSet: Rep, nonTrivial: (DecisionNode<TValue, Atom, Rep>) -> Rep): Rep =
      when (val otherBdd = otherSet.bdd) {
        this -> otherSet
        is Top<*, *, *> -> otherSet
        is Bottom<*, *, *> -> asRep
        is DecisionNode<TValue, Atom, Rep> -> nonTrivial(otherBdd)
      }


    // https://pnwamk.github.io/sst-tutorial/#%28counter._%28figure._fig~3abdd-and%29%29
    override fun overlapWith(otherSet: Rep): Rep = ifNonTrivialOverlapWith(otherSet) { otherDecisionNode ->
      when (discriminator normalizationComparedTo otherDecisionNode.discriminator) {
        isLower -> node(
          discriminator,
          ifTrue overlapWith otherSet,
          regardless overlapWith otherSet,
          ifFalse overlapWith otherSet
        )

        isEqual -> node(
          discriminator,
          (ifTrue with regardless) overlapWith (otherDecisionNode.ifTrue with otherDecisionNode.regardless),
          bottom(),
          (ifFalse with regardless) overlapWith (otherDecisionNode.ifFalse with otherDecisionNode.regardless)
        )

        isHigher -> node(
          otherDecisionNode.discriminator,
          this overlapWith otherDecisionNode.ifTrue,
          this overlapWith otherDecisionNode.regardless,
          this overlapWith otherDecisionNode.ifFalse
        )
      }
    }

    private inline fun ifNonTrivialOverlapWith(otherSet: Rep, nonTrivial: (DecisionNode<TValue, Atom, Rep>) -> Rep): Rep =
      when (val otherBdd = otherSet.bdd) {
        this -> this.asRep
        is Top<*, *, *> -> this.asRep
        is Bottom<*, *, *> -> otherSet
        is DecisionNode<TValue, Atom, Rep> -> nonTrivial(otherBdd)
      }

    // https://pnwamk.github.io/sst-tutorial/#%28counter._%28figure._fig~3abdd-diff%29%29
    // also spells out set difference explicitly. However, we currently reduce it to `a intersectedWith !b`
    // TODO use direct implementation
    override fun without(otherSet: Rep) = this overlapWith !otherSet

    override fun isSubsetOf(largerSet: Rep) = (this without largerSet).isEmpty()

    // https://pnwamk.github.io/sst-tutorial/#%28counter._%28figure._fig~3abdd-not%29%29
    // TODO I _think_ we can just directly call decisionNode() instead of node() at least in some of the cases
    override fun not() = when {
      ifFalse is Bottom<*, *, *> -> node(discriminator, bottom(), !(ifTrue with regardless), !regardless)
      ifTrue is Bottom<*, *, *> -> node(discriminator, !regardless, !(ifFalse with regardless), bottom())
      regardless is Bottom<*, *, *> -> node(discriminator, !ifTrue, !(ifTrue with ifFalse), !ifFalse)
      else -> node(discriminator, !(ifTrue with regardless), bottom(), !(ifFalse with regardless))
    }

    override fun triviallyEquals(otherSet: ClosedSolvableSet<*, *>) =
      this === otherSet || (
        otherSet is DecisionNode<*, *, *>
          && this.discriminator triviallyEquals otherSet.discriminator
          && this.ifTrue triviallyEquals otherSet.ifTrue
          && this.regardless triviallyEquals otherSet.regardless
          && this.ifFalse triviallyEquals otherSet.ifFalse
        )
  }

  abstract class Top<TValue, Atom : ClosedSolvableSet<TValue, in Atom>, Node : BddRepresented<TValue, Atom, Node>> :
    LazyBdd<TValue, Atom, Node> {
    override fun contains(value: TValue) = true
    override fun isEmpty() = false
    override fun isTriviallyEmpty() = false
    override fun isComplete() = true
    override fun isTriviallyComplete() = true
    override fun isFinite() = false
    override fun cardinality(upTo: Int) = moreThanIntMaxValue()

    override val members: Sequence<TValue> get() = throw mustNotRequestMembersOfInfiniteType()

    override fun with(otherSet: Node) = this.asRep

    override fun overlapWith(otherSet: Node) = otherSet

    override fun without(otherSet: Node) = !otherSet

    override fun isSubsetOf(largerSet: Node) = largerSet.isComplete()

    // TODO this will be wrong if a node ever implements SolvableSet
    override fun triviallyEquals(otherSet: ClosedSolvableSet<*, *>) = otherSet.isTriviallyComplete()
  }

  abstract class Bottom<TValue, Atom : ClosedSolvableSet<TValue, in Atom>, Node : BddRepresented<TValue, Atom, Node>> :
    LazyBdd<TValue, Atom, Node> {
    override fun contains(value: TValue) = false
    override fun isEmpty() = true
    override fun isTriviallyEmpty() = true
    override fun isComplete() = false
    override fun isTriviallyComplete() = false
    override fun isFinite() = true
    override fun cardinality(upTo: Int) = exactly(0)
    override fun anyMember() = null
    override val members get() = emptySequence<TValue>()

    override fun with(otherSet: Node) = otherSet

    override fun overlapWith(otherSet: Node) = this.asRep

    override fun without(otherSet: Node) = this.asRep

    override fun isSubsetOf(largerSet: Node) = true

    override fun triviallyEquals(otherSet: ClosedSolvableSet<*, *>) = otherSet.isTriviallyEmpty()
  }

  companion object {
    inline fun <TValue, Atom : ClosedSolvableSet<TValue, in Atom>, Rep : BddRepresented<TValue, Atom, Rep>> node(
      discriminator: Atom,
      ifTrue: Rep,
      regardless: Rep,
      ifFalse: Rep,
      decisionNode: (Atom, Rep, Rep, Rep) -> Rep
    ): Rep = when {
      // TODO check discriminator for trivially empty and trivially complete?
      regardless is Top<*, *, *> -> regardless
      ifTrue triviallyEquals ifFalse -> ifTrue with regardless
      else -> decisionNode(discriminator, ifTrue, regardless, ifFalse)
    }
  }
}
