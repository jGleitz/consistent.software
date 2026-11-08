package software.consistent.data.type.solvers.maps

import software.consistent.data.type.solvers.*
import software.consistent.data.type.solvers.generic.BddRepresented
import software.consistent.data.type.solvers.generic.LazyBdd
import software.consistent.data.type.solvers.maps.SearchOutcome.Companion.continueSearching
import software.consistent.data.type.solvers.maps.SearchOutcome.Companion.stopAndReturn
import software.consistent.data.util.ComparisonCase.*
import software.consistent.data.value.Value
import software.consistent.formatting.monospacelayout.Monospace

internal sealed interface MapSet : BddRepresented<Value.Map, MapComponent, MapSet> {

  /**
   * Progressively transforms this MapSolver into
   * [disjunctive normal form](https://en.wikipedia.org/wiki/Disjunctive_normal_form) and calls the
   * [consumer] to produce the result value from one of the conjunctions.
   *
   * [Disjunctive normal form](https://en.wikipedia.org/wiki/Disjunctive_normal_form) means that the
   * map type is represented as a term of the form `(CAp1 ∧ CAp2 ∧ … ∧ ¬CAn1 ∧ ¬ CAn2 ∧ …) ∨ (CBp1 ∧
   * CBp2 ∧ … ∧ ¬CBn1 ∧ ¬ CBn2 ∧ …) ∨ …`. Each C*** is a [MapComponent]. The [consumer] will be
   * called for each conjunction (that is: for each parenthesized subterm). If the consumer returns
   * [continueSearching], the next conjunction (if any) will be materialized and [consumer] will be
   * called again for it. Else, if the [consumer] returns [stopAndReturn], this method will return
   * it, and the remaining conjunctions (if any) will be ignored.
   *
   * If this map type is empty, [consumer] will never be called and this method will return
   * [notFound][software.consistent.data.type.solvers.maps.SearchOutcome.Companion.notFound]. That
   * also means that any [MapComponentConjunction] passed to [consumer] will have at least one
   * [MapComponentConjunction.positive] component.
   *
   * @return the last value [consumer] returned, or
   *   [notFound][software.consistent.data.type.solvers.maps.SearchOutcome.Companion.notFound] if
   *   the [consumer] was never called.
   */
  fun <Result : Any> searchConjunction(
      consumer: (MapComponentConjunction) -> SearchOutcome<Result>,
  ): SearchOutcome<Result> = searchConjunction(MapComponentConjunction.complete(), consumer)

  fun <Result : Any> searchConjunction(
      accumulator: MapComponentConjunction,
      consumer: (MapComponentConjunction) -> SearchOutcome<Result>,
  ): SearchOutcome<Result>

  private class DecisionNode(
      override val discriminator: MapComponent,
      override val ifTrue: MapSet,
      override val regardless: MapSet,
      override val ifFalse: MapSet,
  ) : LazyBdd.DecisionNode<Value.Map, MapComponent, MapSet>(), MapSet {
    init {
      // TODO better mechanism for checking
      require(!discriminator.isTriviallyEmpty()) {
        "${::discriminator.name} must not be trivially empty – normalise this ${DecisionNode::class.simpleName}!"
      }
      require(!discriminator.isTriviallyComplete()) {
        "${::discriminator.name} must not be trivially complete – normalise this ${DecisionNode::class.simpleName}!"
      }
    }

    override val asRep: MapSet
      get() = this

    override val bdd: LazyBdd<Value.Map, MapComponent, MapSet>
      get() = this

    // TODO check that we don’t have infinite required key types

    override fun node(
        discriminator: MapComponent,
        ifTrue: MapSet,
        regardless: MapSet,
        ifFalse: MapSet,
    ) = MapSet.node(discriminator, ifTrue, regardless, ifFalse)

    override fun top(): MapSet = Top

    override fun bottom(): MapSet = Bottom

    override fun isEmpty() = searchConjunction {
      when {
        it.isEmpty() -> continueSearching()
        else -> stopAndReturn(false)
      }
    }
        .orElse { true }

    override fun isTriviallyEmpty() = false

    override fun isTriviallyNotEmpty() = false

    override fun isFinite() = TODO()

    override fun isComplete() = (!this).isEmpty()

    override fun isTriviallyComplete() = false

    override fun anyMember(): Value.Map? = searchConjunction {
      when (val member = it.anyMember()) {
        null -> continueSearching()
        else -> stopAndReturn(member)
      }
    }
        .orElseNull()

    // potential optimization: paths containing only positive atoms are typically cheaper to
    // calculate on than paths also containing
    // negative atoms. We check paths containing only ‘left’ first, but paths containing only ‘left’
    // and ‘middle’ can also lead to
    // positive-only conjunctions. We could restructure the search to check those before checking
    // any path containing ‘right’.
    override fun <Result : Any> searchConjunction(
        accumulator: MapComponentConjunction,
        consumer: (MapComponentConjunction) -> SearchOutcome<Result>,
    ): SearchOutcome<Result> {
      val ifTrueBranch =
          if (ifTrue === Bottom) continueSearching()
          else {
            val withTrue = accumulator and discriminator
            if (withTrue.isIrreversiblyEmpty()) continueSearching()
            else ifTrue.searchConjunction(withTrue, consumer)
          }

      return ifTrueBranch
          .elseTry { regardless.searchConjunction(accumulator, consumer) }
          .elseTry { ifFalse.searchConjunction(accumulator andNot discriminator, consumer) }
    }

    override val members
      get() = TODO("Not yet implemented")

    override fun cardinality(upTo: Int): Cardinality {
      TODO("Not yet implemented")
    }

    override fun normalizationComparedTo(other: MapSet) =
        when (other) {
          is Bottom -> isHigher
          is Top -> isLower
          is MapComponent -> (this.discriminator normalizationComparedTo other).ifEqual { isHigher }
          is DecisionNode ->
              (this.discriminator normalizationComparedTo other.discriminator)
                  .ifEqual { this.ifTrue normalizationComparedTo other.ifTrue }
                  .ifEqual { this.ifFalse normalizationComparedTo other.ifFalse }
                  .ifEqual { this.regardless normalizationComparedTo other.regardless }
        }

    override val monospaceLayout
      get() =
          Monospace.line("∪", 3) {
            when (ifTrue) {
              Bottom -> Unit
              Top -> -discriminator.monospaceLayout.containerWrappedIn("(", ")")
              else ->
                  -Monospace.line("∩", 2) {
                    -discriminator.monospaceLayout.containerWrappedIn("(", ")")
                    -ifTrue.monospaceLayout.containerWrappedIn("(", ")")
                  }
            }
            when (regardless) {
              Bottom -> Unit
              else -> -regardless.monospaceLayout.containerWrappedIn("(", ")")
            }
            when (ifFalse) {
              Bottom -> Unit
              Top -> -discriminator.monospaceLayout.containerWrappedIn("(", ")")
              else ->
                  -Monospace.line("∩", 2) {
                    -discriminator.monospaceLayout.containerWrappedIn("(", ")")
                    -ifFalse.monospaceLayout.containerWrappedIn("(", ")")
                  }
            }
          }

    override fun toString() = monospaceLayout.renderToString()
  }

  private object Top : LazyBdd.Top<Value.Map, MapComponent, MapSet>(), MapSet {
    override val asRep: MapSet
      get() = this

    override val bdd: LazyBdd<Value.Map, MapComponent, MapSet>
      get() = this

    override fun not(): MapSet = Bottom

    override fun anyMember() = Value.Map()

    override fun <Result : Any> searchConjunction(
        accumulator: MapComponentConjunction,
        consumer: (MapComponentConjunction) -> SearchOutcome<Result>,
    ) = consumer(accumulator)

    override fun normalizationComparedTo(other: MapSet) =
        when (other) {
          is Top -> isEqual
          is Bottom,
          is DecisionNode,
          is MapComponent -> isHigher
        }

    override val monospaceLayout
      get() = Monospace.atom("AnyMap")

    override fun toString() = monospaceLayout.renderToString()
  }

  private object Bottom : LazyBdd.Bottom<Value.Map, MapComponent, MapSet>(), MapSet {
    override val asRep: MapSet
      get() = this

    override val bdd: LazyBdd<Value.Map, MapComponent, MapSet>
      get() = this

    override fun not(): MapSet = Top

    override fun anyMember() = null

    override fun <Result : Any> searchConjunction(
        accumulator: MapComponentConjunction,
        consumer: (MapComponentConjunction) -> SearchOutcome<Result>,
    ) = continueSearching<Result>()

    override fun normalizationComparedTo(other: MapSet) =
        when (other) {
          is Bottom -> isEqual
          is Top,
          is DecisionNode,
          is MapComponent -> isLower
        }

    override val monospaceLayout
      get() = Monospace.atom("∅")

    override fun toString() = monospaceLayout.renderToString()
  }

  companion object {
    internal fun empty(): MapSet = Bottom

    internal fun complete(): MapSet = Top

    internal fun node(
        discriminator: MapComponent,
        ifTrue: MapSet,
        regardless: MapSet,
        ifFalse: MapSet,
    ): MapSet =
        when {
          discriminator.isTriviallyEmpty() -> ifFalse with regardless
          discriminator.isTriviallyComplete() -> ifTrue with regardless
          ifTrue.isTriviallyComplete() &&
              regardless.isTriviallyEmpty() &&
              ifFalse.isTriviallyEmpty() -> discriminator
          else -> LazyBdd.node(discriminator, ifTrue, regardless, ifFalse, ::DecisionNode)
        }

    internal fun just(value: Value.Map): MapSet {
      var remainingKeys = TypePartitionUnion.complete()
      var result = complete()
      for ((key, value) in value.entries) {
        val keyType = Solvers.just(key)
        val component =
            MapComponent(keyType, MaybeAbsent(Solvers.just(value), includesAbsent = false))
        result = result overlapWith component
        remainingKeys = remainingKeys without keyType
      }
      val nothingElseComponent = MapComponent(remainingKeys, MaybeAbsent.onlyAbsent())
      return result overlapWith nothingElseComponent
    }

    internal fun allOf(components: Iterable<MapComponent>) =
        components.fold(complete()) { acc, component -> acc overlapWith component }
  }
}
