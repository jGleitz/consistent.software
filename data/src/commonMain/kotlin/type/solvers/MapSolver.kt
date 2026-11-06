@file:OptIn(ExperimentalContracts::class)

package software.consistent.data.type.solvers

import software.consistent.data.formatting.monospacelayout.Monospace
import software.consistent.data.invariants.checkInvariant
import software.consistent.data.type.solvers.ClosedSolvableSet.Cardinality
import software.consistent.data.type.solvers.MapSolver.Companion.continueSearching
import software.consistent.data.type.solvers.MapSolver.Companion.notFound
import software.consistent.data.type.solvers.MapSolver.Companion.stopAndReturn
import software.consistent.data.value.Value
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.AT_MOST_ONCE
import kotlin.contracts.contract
import kotlin.jvm.JvmInline

internal sealed interface MapSolver : LazyBdd<Value.Map, MapComponent, MapSolver> {
  /**
   * Progressively transforms this MapSolver into [disjunctive normal form](https://en.wikipedia.org/wiki/Disjunctive_normal_form) and calls
   * the [consumer] to produce the result value from one of the conjunctions.
   *
   * [Disjunctive normal form](https://en.wikipedia.org/wiki/Disjunctive_normal_form) means that the map type is represented as
   * a term of the form `(CAp1 ∧ CAp2 ∧ … ∧ ¬CAn1 ∧ ¬ CAn2 ∧ …) ∨ (CBp1 ∧ CBp2 ∧ … ∧ ¬CBn1 ∧ ¬ CBn2 ∧ …) ∨ …`. Each C*** is a [MapComponent].
   * The [consumer] will be called for each conjunction (that is: for each parenthesized subterm). If the consumer returns [continueSearching], the next
   * conjunction (if any) will be materialized and [consumer] will be called again for it. Else, if the [consumer] returns [stopAndReturn],
   * this method will return it, and the remaining conjunctions (if any) will be ignored.
   *
   * If this map type is empty, [consumer] will never be called and this method will return [notFound]. That also means that any [Conjunction]
   * passed to [consumer] will have at least one [Conjunction.positive] component.
   *
   * @return the last value [consumer] returned, or [notFound] if the [consumer] was never called.
   */
  fun <Result : Any> searchConjunction(consumer: (Conjunction) -> SearchOutcome<Result>): SearchOutcome<Result> =
    searchConjunction(Conjunction.complete(), consumer)

  fun <Result : Any> searchConjunction(
    accumulator: Conjunction,
    consumer: (Conjunction) -> SearchOutcome<Result>,
  ): SearchOutcome<Result>

  private class Node(
    discriminator: MapComponent,
    ifTrue: MapSolver,
    regardless: MapSolver,
    ifFalse: MapSolver,
  ) : LazyBdd.Node<Value.Map, MapComponent, MapSolver>(discriminator, ifTrue, regardless, ifFalse),
    MapSolver {
    // TODO check that we don’t have infinite required key types

    override fun node(
      discriminator: MapComponent,
      ifTrue: MapSolver,
      regardless: MapSolver,
      ifFalse: MapSolver,
    ): MapSolver = Node(discriminator, ifTrue, regardless, ifFalse)

    override fun top(): MapSolver = Top

    override fun bottom(): MapSolver = Bottom

    override fun isTriviallyEmpty() = false

    override fun isEmpty() =
      searchConjunction {
        when {
          it.isEmpty() -> continueSearching()
          else -> stopAndReturn(false)
        }
      }.orElse { true }

    override fun isFinite() = TODO()

    override fun isTriviallyComplete() = false

    override fun isComplete() = (!this).isEmpty()

    override fun anyMember(): Value.Map? =
      searchConjunction {
        when (val member = it.anyMember()) {
          null -> continueSearching()
          else -> stopAndReturn(member)
        }
      }.orElseNull()

    // potential optimization: paths containing only positive atoms are typically cheaper to calculate on than paths also containing
    // negative atoms. We check paths containing only ‘left’ first, but paths containing only ‘left’ and ‘middle’ can also lead to
    // positive-only conjunctions. We could restructure the search to check those before checking any path containing ‘right’.
    override fun <Result : Any> searchConjunction(
      accumulator: Conjunction,
      consumer: (Conjunction) -> SearchOutcome<Result>,
    ): SearchOutcome<Result> {
      val withTrue = accumulator and discriminator
      val ifTrueBranch =
        if (withTrue.isIrreversiblyEmpty()) {
          continueSearching()
        } else {
          ifTrue.searchConjunction(withTrue, consumer)
        }

      return ifTrueBranch
        .elseTry { regardless.searchConjunction(accumulator, consumer) }
        .elseTry { ifFalse.searchConjunction(accumulator andNot discriminator, consumer) }
    }

    override val members get() = TODO("Not yet implemented")

    override fun cardinality(upTo: Int): Cardinality {
      TODO("Not yet implemented")
    }

    // TODO use direct implementation
    override fun without(otherSet: MapSolver) = this overlapWith !otherSet

    override fun isSubsetOf(largerSet: MapSolver) = (this without largerSet).isEmpty()

    override val monospaceLayout
      get() =
        Monospace.line("∪", 3) {
          when (ifTrue) {
            Bottom -> {
              Unit
            }

            Top -> {
              -discriminator.monospaceLayout.containerWrappedIn("(", ")")
            }

            else -> {
              -Monospace.line("∩", 2) {
                -discriminator.monospaceLayout.containerWrappedIn("(", ")")
                -ifTrue.monospaceLayout.containerWrappedIn("(", ")")
              }
            }
          }
          when (regardless) {
            Bottom -> Unit
            else -> -regardless.monospaceLayout.containerWrappedIn("(", ")")
          }
          when (ifFalse) {
            Bottom -> {
              Unit
            }

            Top -> {
              -discriminator.monospaceLayout.containerWrappedIn("(", ")")
            }

            else -> {
              -Monospace.line("∩", 2) {
                -discriminator.monospaceLayout.containerWrappedIn("(", ")")
                -ifFalse.monospaceLayout.containerWrappedIn("(", ")")
              }
            }
          }
        }

    override fun toString() = monospaceLayout.renderToString()
  }

  private object Top : LazyBdd.Top<Value.Map, MapComponent, MapSolver>(), MapSolver {
    override fun not(): MapSolver = Bottom

    override fun anyMember() = Value.Map()

    override fun <Result : Any> searchConjunction(
      accumulator: Conjunction,
      consumer: (Conjunction) -> SearchOutcome<Result>,
    ) = consumer(accumulator)

    override val monospaceLayout get() = Monospace.atom("AnyMap")

    override fun toString() = monospaceLayout.renderToString()
  }

  private object Bottom : LazyBdd.Bottom<Value.Map, MapComponent, MapSolver>(), MapSolver {
    override fun not(): MapSolver = Top

    override fun anyMember() = null

    override fun <Result : Any> searchConjunction(
      accumulator: Conjunction,
      consumer: (Conjunction) -> SearchOutcome<Result>,
    ) = continueSearching<Result>()

    override val monospaceLayout get() = Monospace.atom("∅")

    override fun toString() = monospaceLayout.renderToString()
  }

  /**
   * Semantically, these collections are sets. But the invariants of the LazyBdd and this class guarantee that there will be no duplicates.
   * Hence, we use arrays, which are cheaper to copy.
   */
  class Conjunction(
    /**
     * The positive atoms of the conjunction, split such that each component’s key type in disjunct from the key type of any other component.
     */
    val positive: KeyDisjunctMapComponents,
    /**
     * All keys that are not covered by any key type of a [positive] component.
     */
    val uncoveredKeys: SolvableSet,
    /**
     * The negative atoms of the conjunction. Their key types may overlap.
     */
    val negative: List<MapComponent>,
  ) {
    /*
     * trade-off: We currently split the positive components into disjunct keys while walking down the LazyBdd. This allows us to abort
     * early if we encounter a contradiction in the value types. It also saves us work compared to calculating the key-disjunct atoms at
     * every LazyBdd leaf (if we visit many leaves). On the other hand, calculating the full value type intersection is more work than is
     * strictly needed just to find contradictions.
     * One optimization might be to only do the splitting once we reach a Top leaf but in a way that still allows re-use of the intermediate
     *  results.
     */
    infix fun and(component: MapComponent): Conjunction {
      val positiveWithComponent = positive + component
      val lastAdded = positiveWithComponent.lastOrNull()

      // because positiveWithComponent is disjunct, the last added component is exactly what is new in the key type of component
      val newUncovered =
        if (lastAdded != null && lastAdded != positive.lastOrNull()) {
          uncoveredKeys without lastAdded.keys
        } else {
          uncoveredKeys
        }

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
      // TODO better method for checking.
      require(!isIrreversiblyEmpty()) { "${::anyMember.name}() called on irreversibly empty conjunction!" }
      // A member needs to come from `⋃positive ∖ ⋃negative`. We know at this point that ⋃positive is not empty. Since its keys are disjunct,
      // this means they allow at least one member. We check the negative clauses progressively so we can abort the search as soon as we
      // discover that they cancel out each positive member.
      return TODO()
    }

    fun isEmpty(): Boolean {
      require(!isIrreversiblyEmpty()) { "${::isEmpty.name}() called on irreversibly empty conjunction!" } // TODO better method for checking
      if (negative.isEmpty()) return false

      val satisfier = NegativesSatisfier()
      return searchPartition { keys, positiveValues, negatives ->
        val _ = satisfier.addCandidate(keys, positiveValues, negatives)
        when {
          satisfier.allNegativesSatisfied() -> stopAndReturn(false)
          else -> continueSearching()
        }
      }.orElse {
        return !satisfier.canSatisfyAllNegatives()
      }
    }

    private fun <R : Any> searchPartition(
      consumer: (keys: SolvableSet, positiveValues: MaybeAbsent, negatives: NegativeMapComponents) -> SearchOutcome<R>,
    ): SearchOutcome<R> {
      var outcome: SearchOutcome<R> = notFound()

      for ((keys, values) in positive) {
        outcome = searchPartition(keys, values, NegativeMapComponents(negative.size), 0, consumer)
        if (outcome.isFound()) return outcome
      }

      if (!uncoveredKeys.isEmpty()) {
        outcome = searchPartition(uncoveredKeys, MaybeAbsent.complete(), NegativeMapComponents(negative.size), 0, consumer)
      }
      return outcome
    }

    private fun <R : Any> searchPartition(
      keys: SolvableSet,
      positiveValues: MaybeAbsent,
      negativeMapComponents: NegativeMapComponents,
      negativesIndex: Int,
      consumer: (keys: SolvableSet, positiveValues: MaybeAbsent, negatives: NegativeMapComponents) -> SearchOutcome<R>,
    ): SearchOutcome<R> =
      if (negativesIndex == negative.size) {
        consumer(keys, positiveValues, negativeMapComponents.copy())
      } else {
        val negative = negative[negativesIndex]
        val negativeKeyOverlap = keys overlapWith negative.keys
        if (negativeKeyOverlap.isEmpty()) {
          searchPartition(keys, positiveValues, negativeMapComponents, negativesIndex + 1, consumer)
        } else {
          negativeMapComponents
            .with(negativesIndex) { withNegative ->
              searchPartition(negativeKeyOverlap, positiveValues, withNegative, negativesIndex + 1, consumer)
            }.elseTry {
              val withoutNegativeKeys = keys without negative.keys
              if (!withoutNegativeKeys.isEmpty()) {
                searchPartition(withoutNegativeKeys, positiveValues, negativeMapComponents, negativesIndex + 1, consumer)
              } else {
                notFound()
              }
            }
        }
      }

    @JvmInline
    internal value class KeyDisjunctMapComponents private constructor(
      /**
       * notable cases:
       *   - `components == [MapComoponent.empty()]` represents the empty (that is: uninhabited) map set. All uninhabited variants are
       *      normalized to this representation.
       *   - `components == []` represents the set containing all maps.
       */
      private val components: List<MapComponent>,
    ) : Iterable<MapComponent> by components {
      init {
        ::components.checkInvariant("contains no empty key or value types (except MapComponent.empty())") { components ->
          components.none { (it.keys.isEmpty() || it.values.isEmpty()) && it != MapComponent.empty() }
        }
      }

      operator fun plus(component: MapComponent): KeyDisjunctMapComponents {
        if (component.values.isEmpty()) return empty()

        return KeyDisjunctMapComponents(
          buildList {
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
              toAdd =
                if (!toAddKeysWithoutOverlap.isEmpty()) {
                  toAdd.copy(keys = toAddKeysWithoutOverlap)
                } else {
                  null
                }
            }

            if (toAdd != null) add(toAdd)
          },
        )
      }

      fun lastOrNull() = components.lastOrNull()

      fun isEmpty(): Boolean = components.size == 1 && components[0] == MapComponent.empty()

      companion object {
        internal fun empty(): KeyDisjunctMapComponents = KeyDisjunctMapComponents(listOf(MapComponent.empty()))

        internal fun complete(): KeyDisjunctMapComponents = KeyDisjunctMapComponents(emptyList())
      }
    }

    private class NegativeMapComponents(
      private val contained: BooleanArray,
      private var _size: Int,
    ) {
      constructor(maxIndex: Int) : this(BooleanArray(maxIndex), 0)

      val size: Int get() = _size

      fun isEmpty(): Boolean = _size == 0

      fun isNotEmpty(): Boolean = _size > 0

      fun set(negativeIndex: Int) {
        if (!contained[negativeIndex]) {
          contained[negativeIndex] = true
          _size += 1
        }
      }

      fun setAll(otherComponents: NegativeMapComponents) {
        otherComponents.forEachIndex(this::set)
      }

      fun unset(negativeIndex: Int) {
        if (contained[negativeIndex]) {
          contained[negativeIndex] = false
          _size -= 1
        }
      }

      operator fun get(negativeIndex: Int) = contained[negativeIndex]

      inline fun <R> with(
        negativeIndex: Int,
        block: (NegativeMapComponents) -> R,
      ): R {
        val previous = this[negativeIndex]
        return if (previous) {
          block(this)
        } else {
          set(negativeIndex)
          block(this)
            .also { unset(negativeIndex) }
        }
      }

      fun copy(): NegativeMapComponents = NegativeMapComponents(contained.copyOf(), size)

      inline fun forEachIndex(indexConsumer: (index: Int) -> Unit) {
        var index = 0
        var found = 0
        while (found < _size) {
          if (contained[index]) {
            indexConsumer(index)
            found += 1
          }
          index += 1
        }
      }

      @IgnorableReturnValue
      // TODO clarify whether the return value indicates that an index was removed.
      inline fun removeIndexIf(predicate: (Int) -> Boolean): Boolean {
        var index = 0
        var retained = 0
        val previousSize = _size
        while (retained < _size) {
          if (contained[index]) {
            if (predicate(index)) {
              unset(index)
            } else {
              retained += 1
            }
          }
          index += 1
        }
        return previousSize != _size
      }

      fun iterator(negatives: List<MapComponent>): MutableIterator<MapComponent> =
        object : MutableIterator<MapComponent> {
          private var index = -1
          private var returned = 0

          override fun hasNext(): Boolean = returned < _size

          override fun next(): MapComponent {
            if (!hasNext()) throw NoSuchElementException() // todo better check
            do {
              index += 1
            } while (!contained[index])
            returned += 1
            return negatives[index]
          }

          override fun remove() {
            require(index >= 0) { "Cannot remove from iterator that has not been advanced" } // todo better check
            require(contained[index]) { "Element is already removed" }
            unset(index)
            returned -= 1
          }
        }
    }

    private operator fun NegativeMapComponents.iterator(): MutableIterator<MapComponent> = iterator(this@Conjunction.negative)

    @IgnorableReturnValue
    // TODO clarify whether the return value indicates a removal.
    private inline fun NegativeMapComponents.removeIf(predicate: (MapComponent) -> Boolean): Boolean {
      val negativeIt = iterator()
      val previousSize = size
      while (negativeIt.hasNext()) {
        if (predicate(negativeIt.next())) negativeIt.remove()
      }
      return previousSize != size
    }

    // TODO this class assumes that .cardinality() is cheap – we should probably cache it
    private inner class NegativesSatisfier(
      // overall, it feel like there is a much more efficient data structure for this
      // candidatesPerNegative[index] == null iff satisfied[index] == true
      private val candidatesPerNegative: Array<MutableSet<Int>?> = Array(negative.size) { HashSet() },
      private val candidates: MutableList<Partition?> = mutableListOf(),
      private var toSatisfy: Int = negative.size,
    ) {
      fun addCandidate(
        keys: SolvableSet,
        positiveValues: MaybeAbsent,
        satisfiableNegatives: NegativeMapComponents,
      ): PropagationOutcome {
        satisfiableNegatives.removeIndexIf(this::isSatisfied)
        satisfiableNegatives.removeIf {
          // The negative forbids every positive value of this partition -> we will not be able to satisfy the negative in this partition
          // we rely on the fact that each candidate can actually satisfy a negative, so we cannot us isTriviallyEmpty here.
          (positiveValues without it.values).isEmpty()
        }
        if (satisfiableNegatives.isEmpty()) return moreToSatisfy()

        val keyCardinality = keys.cardinality(satisfiableNegatives.size)
        return if (keyCardinality >= satisfiableNegatives.size) {
          // We have enough keys to use a new key to satisfy each of the remaining negatives. Hence, we can satisfy them all with
          // this partition and store that eagerly. There’s also no need to store this partition, it did all it could do.
          markSatisfiedAndPropagate(satisfiableNegatives)
        } else {
          val candidateIndex = candidates.size
          candidates += Partition(keys, positiveValues, satisfiableNegatives, keyCardinality.exactValue)
          satisfiableNegatives.forEachIndex {
            // if candidatesPerNegative[it] was null, satisfied[it] would be true and this negative would have been removed at the start
            // of this method.
            candidatesPerNegative[it]!!.add(candidateIndex)
          }
          moreToSatisfy()
        }
      }

      fun canSatisfyAllNegatives(): Boolean {
        for ((negative, candidates) in candidatesPerNegative.withIndex()) {
          if (candidates == null) continue
          when (candidates.size) {
            0 -> {
              return false
            }

            1 -> {
              satisfyUsing(negative, candidates.single())
                .ifDecided { return it.isDefinitelySatisfiable() }
            }
          }
        }

        return canSatisfyAll()
      }

      private fun canSatisfyAll(): Boolean {
        if (allNegativesSatisfied()) return true

        var negative = -1
        var candidates: MutableSet<Int>? = null
        while (candidates == null) {
          negative += 1
          candidates = candidatesPerNegative[negative]
        }

        for (candidateIndex in candidates) {
          val copy = copy()
          copy
            .satisfyUsing(negative, candidateIndex)
            .ifDecided { if (it.isDefinitelySatisfiable()) return true else continue }
          if (copy.canSatisfyAll()) return true
        }
        return false
      }

      private fun satisfyUsing(
        negative: Int,
        candidateIndex: Int,
      ): PropagationOutcome {
        val candidate = candidates[candidateIndex]!!
        // TODO check invariant: negative is satisfiable by candidate
        candidate.remainingKeys -= 1
        markSatisfiedAndPropagate(negative)
          .ifDecided { return it }

        // the key we used to satisfy the negative might also satisfy another negative. If that’s possible, we need to add a candidate
        // for this possibility.

        // this cannot be empty, otherwise this would not be a candidate for this negative. We might still want to check that invariant, though
        val remainingValues = candidate.positiveValues without this@Conjunction.negative[negative].values
        val remainingSatisfiableNegatives = candidate.satisfiableNegatives.copy()
        remainingSatisfiableNegatives.removeIf {
          // The negative forbids every remaining positive value -> we will not be able to satisfy the negative with the remainder
          (remainingValues without it.values).isEmpty()
        }
        if (remainingSatisfiableNegatives.isNotEmpty()) {
          val remainderCandidateIndex = candidates.size
          candidates += Partition(candidate.keys, remainingValues, remainingSatisfiableNegatives, 1)
          remainingSatisfiableNegatives.forEachIndex { negative ->
            // the set cannot be null, it must contain at least the candidate
            candidatesPerNegative[negative]!!.add(remainderCandidateIndex)
          }
        }

        if (candidate.remainingKeys == 0) {
          // we check if any negative becomes unsatisfiable in this call, hence we must only do it after we have added the potential
          // remainder candidate
          removeDepletedCandidateAndPropagate(candidateIndex)
            .ifDecided { return it }
        }

        return moreToSatisfy()
      }

      private fun markAndPropagateSatisfiesAllNegatives(partition: Partition): PropagationOutcome {
        // this is a cheap trick to make sure canTriviallySatisfyAllNegatives() will never return true which could trigger another
        // markAndPropagateSatisfiesAll for it. The partition will be removed from all data structures, so the sentinel value will never
        // be used after this method returns.
        partition.remainingKeys = -1
        return markSatisfiedAndPropagate(partition.satisfiableNegatives)
      }

      private fun markSatisfiedAndPropagate(negatives: NegativeMapComponents): PropagationOutcome {
        negatives.forEachIndex { negative ->
          markSatisfiedAndPropagate(negative)
            .ifDecided { return it }
        }
        return moreToSatisfy()
      }

      private fun markSatisfiedAndPropagate(satisfiedNegative: Int): PropagationOutcome {
        val candidatesForNegative = candidatesPerNegative[satisfiedNegative]
        if (candidatesForNegative != null) {
          candidatesPerNegative[satisfiedNegative] = null
          toSatisfy -= 1
          if (allNegativesSatisfied()) return definitelySatisfiable()

          for (candidateIndex in candidatesForNegative) {
            val candidate = candidates[candidateIndex]!!
            candidate.satisfiableNegatives.unset(satisfiedNegative)
            if (candidate.canTriviallySatisfyAllNegatives()) {
              markAndPropagateSatisfiesAllNegatives(candidate)
                .ifDecided { return it }
            }
          }
        }
        return moreToSatisfy()
      }

      private fun removeDepletedCandidateAndPropagate(candidateIndex: Int): PropagationOutcome {
        val candidate = candidates.set(candidateIndex, null)!!
        candidate.satisfiableNegatives.forEachIndex { negative ->
          // this set cannot be null because at least candidate is in it
          candidatesPerNegative[negative]!!.remove(candidateIndex)
        }
        candidate.satisfiableNegatives.forEachIndex { negative ->
          // this set cannot be null because at least candidate was in it
          val otherCandidates = candidatesPerNegative[negative]!!
          when (otherCandidates.size) {
            0 -> {
              return definitelyUnsatisfiable()
            }

            1 -> {
              // this negative can only be satisfied by one candidate. So we can eagerly mark that.
              satisfyUsing(negative, otherCandidates.single())
                .ifDecided { return it }
            }
          }
        }
        return moreToSatisfy()
      }

      fun copy() =
        NegativesSatisfier(
          Array(this.candidatesPerNegative.size) {
            candidatesPerNegative[it]?.let(::HashSet)
          },
          MutableList(candidates.size) { candidates[it]?.copy() },
          toSatisfy,
        )

      private fun isSatisfied(negativeIndex: Int) = candidatesPerNegative[negativeIndex] == null

      fun allNegativesSatisfied() = toSatisfy == 0

      fun anyNegativeUnsatisfiable() = candidatesPerNegative.any { it != null && it.isEmpty() }
    }

    private class Partition(
      val keys: SolvableSet,
      val positiveValues: MaybeAbsent,
      val satisfiableNegatives: NegativeMapComponents,
      var remainingKeys: Int,
    ) {
      // We have enough keys to use a new key to satisfy each of the remaining negatives.
      fun canTriviallySatisfyAllNegatives() = remainingKeys >= satisfiableNegatives.size

      fun copy(): Partition = Partition(keys, positiveValues, satisfiableNegatives.copy(), remainingKeys)
    }

    @JvmInline
    private value class PropagationOutcome(
      // 0 -> undecided
      // 1 -> unsatisfiable
      // 2 -> satisfiable
      private val state: Byte,
    ) {
      inline fun ifDecided(consumer: (outcome: PropagationOutcome) -> Unit) {
        contract {
          callsInPlace(consumer, AT_MOST_ONCE)
        }
        if (state != 0.toByte()) consumer(this)
      }

      fun isDefinitelySatisfiable() = state == 2.toByte()
    }

    private fun definitelySatisfiable() = PropagationOutcome(2)

    private fun definitelyUnsatisfiable() = PropagationOutcome(1)

    private fun moreToSatisfy() = PropagationOutcome(0)

    companion object {
      private val empty =
        Conjunction(
          positive = KeyDisjunctMapComponents.empty(),
          uncoveredKeys = PartitionedTypeUnion.empty(),
          negative = emptyList(),
        )
      private val complete =
        Conjunction(
          positive = KeyDisjunctMapComponents.complete(),
          uncoveredKeys = PartitionedTypeUnion.complete(),
          negative = emptyList(),
        )

      internal fun empty() = empty

      internal fun complete() = complete
    }
  }

  companion object {
    internal fun empty(): MapSolver = Bottom

    internal fun complete(): MapSolver = Top

    internal fun just(component: MapComponent): MapSolver =
      Node(
        discriminator = component,
        ifTrue = Top,
        regardless = Bottom,
        ifFalse = Bottom,
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

    private fun <T : Any> stopAndReturn(value: T): SearchOutcome<T> = SearchOutcome(value)

    private fun <T : Any> continueSearching(): SearchOutcome<T> = SearchOutcome(null)

    private fun <T : Any> notFound(): SearchOutcome<T> = SearchOutcome(null)
  }

  @JvmInline
  value class SearchOutcome<T : Any>(
    val result: T?,
  ) {
    inline fun elseTry(block: () -> SearchOutcome<T>): SearchOutcome<T> = result?.let { SearchOutcome(it) } ?: block()

    inline fun orElse(fallback: () -> T): T = result ?: fallback()

    fun orElseNull(): T? = result

    fun isFound(): Boolean = result != null
  }
}
