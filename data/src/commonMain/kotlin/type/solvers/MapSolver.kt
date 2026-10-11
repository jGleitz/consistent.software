package software.consistent.data.type.solvers

import software.consistent.data.invariants.checkInvariant
import software.consistent.data.value.Value
import kotlin.jvm.JvmInline

internal sealed interface MapSolver : LazyBdd<Value.Map, MapComponent, MapSolver> {

  /**
   * Progressively transforms this MapSolver into [disjunctive normal form](https://en.wikipedia.org/wiki/Disjunctive_normal_form) and calls
   * the [consumer] to produce the result value from one of the conjunctions.
   *
   * [Disjunctive normal form](https://en.wikipedia.org/wiki/Disjunctive_normal_form) means that the map type is represented as
   * a term of the form `(CAp1 ∧ CAp2 ∧ … ∧ ¬CAn1 ∧ ¬ CAn2 ∧ …) ∨ (CBp1 ∧ CBp2 ∧ … ∧ ¬CBn1 ∧ ¬ CBn2 ∧ …) ∨ …`. Each C*** is a [MapComponent].
   * The [consumer] will be called for each conjunction (that is: for each parenthesized subterm). If the consumer returns `null`, the next
   * conjunction (if any) will be materialized and [consumer] will be called again for it. If the [consumer] returns anything but `null`,
   * this method will return and the remaining conjunctions (if any) will be ignored. This method will return whatever [consumer]
   * returned the last time it was called.
   *
   * If this map type is empty, [consumer] will never be called and this method will return `null`. That also means that any [Conjunction]
   * passed to [consumer] will have at least one [Conjunction.positive] component.
   */
  fun <Result : Any> searchConjunction(consumer: (Conjunction) -> Result?): Result? =
    searchConjunction(Conjunction.complete(), consumer)

  fun <Result : Any> searchConjunction(accumulator: Conjunction, consumer: (Conjunction) -> Result?): Result?

  private class Node(
    discriminator: MapComponent,
    ifTrue: MapSolver,
    regardless: MapSolver,
    ifFalse: MapSolver
  ) : LazyBdd.Node<Value.Map, MapComponent, MapSolver>(discriminator, ifTrue, regardless, ifFalse), MapSolver {
    // TODO check that we don’t have infinite required key types

    override fun node(
      discriminator: MapComponent,
      ifTrue: MapSolver,
      regardless: MapSolver,
      ifFalse: MapSolver
    ): MapSolver = Node(discriminator, ifTrue, regardless, ifFalse)

    override fun top(): MapSolver = Top

    override fun bottom(): MapSolver = Bottom

    override fun isEmpty() = searchConjunction { it.isEmpty().takeIf { r -> !r } } ?: true

    override fun isComplete() = (!this).isEmpty()

    override fun anyMember(): Value.Map? = searchConjunction { it.anyMember() }

    // potential optimization: paths containing only positive atoms are typically cheaper to calculate on than paths also containing
    // negative atoms. We check paths containing only ‘left’ first, but paths containing only ‘left’ and ‘middle’ can also lead to
    // positive-only conjunctions. We could restructure the search to check those before checking any path containing ‘right’.
    override fun <Result : Any> searchConjunction(
      accumulator: Conjunction,
      consumer: (Conjunction) -> Result?
    ): Result? {
      val withTrue = accumulator and discriminator
      val ifTrueBranch = if (withTrue.isIrreversiblyEmpty()) null
      else ifTrue.searchConjunction(withTrue, consumer)

      return ifTrueBranch
        ?: regardless.searchConjunction(accumulator, consumer)
        ?: ifFalse.searchConjunction(accumulator andNot discriminator, consumer)
    }

    override val members get() = TODO("Not yet implemented")

    override fun isFinite(): Boolean {
      TODO("Not yet implemented")
    }

    override fun cardinality(max: Int): Int {
      TODO("Not yet implemented")
    }

    // TODO use direct implementation
    override fun without(otherSet: MapSolver) = this overlapWith !otherSet

    override fun isSubsetOf(largerSet: MapSolver) = (this without largerSet).isEmpty()
  }

  private object Top : LazyBdd.Top<Value.Map, MapComponent, MapSolver>(), MapSolver {
    override fun not(): MapSolver = Bottom

    override fun anyMember() = Value.Map()

    override fun <Result : Any> searchConjunction(
      accumulator: Conjunction,
      consumer: (Conjunction) -> Result?
    ) = consumer(accumulator)
  }

  private object Bottom : LazyBdd.Bottom<Value.Map, MapComponent, MapSolver>(), MapSolver {
    override fun not(): MapSolver = Top

    override fun anyMember() = null

    override fun <Result : Any> searchConjunction(
      accumulator: Conjunction,
      consumer: (Conjunction) -> Result?
    ) = null
  }

  class Conjunction(
    /*
     * Semantically, these collections are sets. But the invariants of the LazyBdd and this class guarantee that there will be no duplicates.
     * Hence, we use arrays, which are cheaper to copy.
     */
    /**
     * The positive atoms of the conjunction, split such that each component’s key type in disjunct from the key type of any other component.
     */
    val positive: DisjunctKeyMapComponents,
    /**
     * All keys that are not covered by any key type of a [positive] component.
     */
    val uncoveredKeys: SolvableSet,
    /**
     * The negative atoms of the conjunction. Their key types may overlap.
     */
    val negative: List<MapComponent>
  ) {
    /*
     * trade-off: We currently split the positive components into disjunct keys while walking down the LazyBdd. This allows us to abort
     * early if we encounter a contradiction in the value types. It also saves us work compared to calculating the key-disjunct atoms at
     * every LazyBdd leaf (if we visit many leaves). On the other hand, calculating the full value type intersection is more work than is
     * strictly needed just to find contradictions.
     */
    infix fun and(component: MapComponent): Conjunction {
      val positiveWithComponent = positive + component
      val lastAdded = positiveWithComponent.lastOrNull()

      // because positiveWithComponent is disjunct, the last added component is exactly what is new in the key type of component
      val newUncovered = if (lastAdded != null && lastAdded != positive.lastOrNull()) uncoveredKeys without lastAdded.keys
      else uncoveredKeys

      return Conjunction(positiveWithComponent, newUncovered, negative)
    }

    infix fun andNot(comp: MapComponent) = Conjunction(positive, uncoveredKeys, negative + comp)

    /**
     * Return `true` iff this conjunction is known to always be empty, regardless of which components are added to it with [and] or
     * [andNot]. This is the case, for example, when the positive atoms specify disjunct value types for a required key.
     *
     * If this method returns `false` the conjunction may still be empty.
     */
    fun isIrreversiblyEmpty() = positive.isEmpty()

    fun anyMember(): Value.Map? {
      require(!isIrreversiblyEmpty()) { "${::anyMember.name}() called on irreversibly empty conjunction!" } // TODO better method for checking
      // A member needs to come from `⋃positive ∖ ⋃negative`. We know at this point that ⋃positive is not empty. Since its keys are disjunct,
      // this means they allow at least one member. We check the negative clauses progressively so we can abort the search as soon as we
      // discover that they cancel out each positive member.
      return Value.Map(buildMap {
        searchPartitions { positive, negatives -> addAllowedComponents(positive, negatives) }
          ?.let { return null }
      })
    }

    private fun MutableMap<Value, Value>.addAllowedComponents(positive: MapComponent, negatives: List<MapComponent>): Boolean? {
      var remainingValues = positive.values // cannot be empty per our invariant
      for (negative in negatives) {
        // TODO explain this, it’s really hard
        val remainingWithoutNegative = remainingValues without negative.values
        if (remainingWithoutNegative.isEmpty()) {
          return false
        }
        remainingValues = remainingWithoutNegative
      }
      if (!remainingValues.includesAbsent) {
        // we know the remaining values aren’t empty and they don’t include absent, so they must include at least one real value
        val value = remainingValues.values.anyMember()!!
        // per our invariants, positives are finite if they don’t include absent
        for (key in positive.keys.members) put(key, value)
      }
      return null
    }

    fun isEmpty(): Boolean {
      require(!isIrreversiblyEmpty()) { "${::isEmpty.name}() called on irreversibly empty conjunction!" } // TODO better method for checking
      return negative.isNotEmpty()
        && searchPartitions { positive, negatives -> partitionIsEmpty(positive, negatives).takeIf { r -> !r } } ?: true
    }

    private fun partitionIsEmpty(positive: MapComponent, negatives: List<MapComponent>): Boolean {
      if (negatives.isEmpty()) return false
      var remainingValues = positive.values
      for ((_, negativeValues) in negatives) {
        // TODO explain this, it’s really hard
        val remainingWithoutNegative = remainingValues without negativeValues
        if (remainingWithoutNegative.isEmpty()) {
          return true
        } else if (positive.keys.isFinite() && positive.keys.cardinality(negatives.size) < negatives.size) {
          // TODO it’s unclear to me whether the cardinality check does any good here, since we had to calculate remainingWithoutNegative already.
          // Why wouldn’t we want to make the remaining values smaller?
          remainingValues = remainingWithoutNegative
        }
      }
      return false
    }

    private fun <Result : Any> searchPartitions(
      consumer: (positive: MapComponent, negatives: List<MapComponent>) -> Result?
    ): Result? {
      for (positive in positive) {
        searchPartitions(positive, emptyList(), negative.listIterator(), consumer)
          ?.let { return it }
      }
      if (!uncoveredKeys.isEmpty()) {
        val uncoveredComponent = MapComponent(uncoveredKeys, MaybeAbsent.complete())
        searchPartitions(uncoveredComponent, emptyList(), negative.listIterator(), consumer)
          ?.let { return it }
      }
      return null
    }

    private fun <Result : Any> searchPartitions(
      positive: MapComponent,
      coveredNegatives: List<MapComponent>,
      negativesIterator: ListIterator<MapComponent>,
      consumer: (positive: MapComponent, negatives: List<MapComponent>) -> Result?
    ): Result? = if (!negativesIterator.hasNext()) {
      consumer(positive, coveredNegatives)
    } else negativesIterator.withNext { negative ->
      val positiveNegativeKeyOverlap = positive.keys overlapWith negative.keys
      if (!positiveNegativeKeyOverlap.isEmpty()) {
        searchPartitions(
          positive.copy(keys = positiveNegativeKeyOverlap),
          coveredNegatives + negative,
          negativesIterator,
          consumer
        )
          ?: searchPartitions(
            positive.copy(keys = positive.keys without negative.keys),
            coveredNegatives,
            negativesIterator,
            consumer
          )
      } else searchPartitions(positive, coveredNegatives, negativesIterator, consumer)
    }

    private inline fun <T, R> ListIterator<T>.withNext(consumer: (T) -> R): R = consumer(next()).also {
      @Suppress("RETURN_VALUE_NOT_USED_COERCION")
      previous()
    }

    @JvmInline
    internal value class DisjunctKeyMapComponents private constructor(
      /**
       * notable cases:
       *   - `components == [MapComoponent.empty()]` represents the empty (that is: uninhabited) map set. All uninhabited variants are
       *      normalized to this representation.
       *   - `components == []` represents the set containing all maps.
       */
      private val components: List<MapComponent>
    ) : Iterable<MapComponent> by components {
      init {
        ::components.checkInvariant("contains no empty key or value types (except MapComponent.empty())") { components ->
          components.none { (it.keys.isEmpty() || it.values.isEmpty()) && it != MapComponent.empty() }
        }
      }

      operator fun plus(component: MapComponent): DisjunctKeyMapComponents =
        DisjunctKeyMapComponents(buildList {
          var toAdd: MapComponent? = component

          for (preExisting in components) {
            if (toAdd == null) {
              add(preExisting)
              continue
            }

            val keyOverlap = preExisting.keys overlapWith toAdd.keys
            if (keyOverlap.isEmpty()) {
              add(preExisting)
              continue
            }

            val valueOverlap = preExisting.values overlapWith toAdd.values
            if (valueOverlap.isEmpty()) {
              // if any key is required but the effective value type is empty, the whole map type is empty
              return@plus empty()
            }
            add(MapComponent(keyOverlap, valueOverlap))

            val preExistingKeysWithoutOverlap = preExisting.keys without keyOverlap
            if (!preExistingKeysWithoutOverlap.isEmpty()) {
              add(preExisting.copy(keys = preExistingKeysWithoutOverlap))
            }

            val toAddKeysWithoutOverlap = toAdd.keys without keyOverlap
            toAdd = if (!toAddKeysWithoutOverlap.isEmpty()) toAdd.copy(keys = toAddKeysWithoutOverlap)
            else null
          }

          if (toAdd != null) add(toAdd)
        })

      fun lastOrNull() = components.lastOrNull()

      fun isEmpty(): Boolean = components.size == 1 && components[0] == MapComponent.empty()

      companion object {
        internal fun empty(): DisjunctKeyMapComponents = DisjunctKeyMapComponents(listOf(MapComponent.empty()))
        internal fun complete(): DisjunctKeyMapComponents = DisjunctKeyMapComponents(emptyList())
      }
    }

    companion object {
      private val empty = Conjunction(
        positive = DisjunctKeyMapComponents.empty(),
        uncoveredKeys = PartitionedTypeUnion.empty(),
        negative = emptyList()
      )
      private val complete = Conjunction(
        positive = DisjunctKeyMapComponents.complete(),
        uncoveredKeys = PartitionedTypeUnion.complete(),
        negative = emptyList()
      )

      internal fun empty() = empty
      internal fun complete() = complete
    }
  }

  companion object {
    internal fun empty(): MapSolver = Bottom
    internal fun complete(): MapSolver = Top
    internal fun just(component: MapComponent): MapSolver = Node(
      discriminator = component,
      ifTrue = Top,
      regardless = Bottom,
      ifFalse = Bottom
    )

    internal fun just(value: Value.Map): MapSolver {
      var remainingKeys = PartitionedTypeUnion.complete()
      var result = complete()
      for ((key, value) in value.entries) {
        val keyType = Solvers.just(key)
        val component = MapComponent(keyType, MaybeAbsent(Solvers.just(value), includesAbsent = false))
        result = result overlapWith just(component)
        remainingKeys = remainingKeys without keyType
      }
      val nothingElseComponent = MapComponent(remainingKeys, MaybeAbsent.onlyAbsent())
      return result overlapWith just(nothingElseComponent)
    }

    internal fun allOf(components: Iterable<MapComponent>) =
      components.fold(complete()) { acc, component -> acc overlapWith just(component) }
  }
}
