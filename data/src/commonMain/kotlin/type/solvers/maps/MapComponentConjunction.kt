@file:OptIn(ExperimentalContracts::class)

package software.consistent.data.type.solvers.maps

import software.consistent.data.invariants.checkInvariant
import software.consistent.data.type.solvers.SolvableSet
import software.consistent.data.type.solvers.TypePartitionUnion
import software.consistent.data.type.solvers.maps.SearchOutcome.Companion.continueSearching
import software.consistent.data.type.solvers.maps.SearchOutcome.Companion.notFound
import software.consistent.data.type.solvers.maps.SearchOutcome.Companion.stopAndReturn
import software.consistent.data.value.Value
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.jvm.JvmInline

internal class MapComponentConjunction(
  /*
   * Semantically, these collections are sets. But the invariants of the LazyBdd and this class guarantee that there will be no duplicates.
   * Hence, we use arrays, which are cheaper to copy.
   */
  /**
   * The positive atoms of the conjunction, split such that each component’s key type in disjunct from the key type of any other component.
   */
  val positive: KeyDisjointMapComponents,
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
   * One optimization might be to only do the splitting once we reach a Top leaf but in a way that still allows re-use of the intermediate
   *  results.
   */
  infix fun and(component: MapComponent): MapComponentConjunction {
    val positiveWithComponent = positive + component
    val lastAdded = positiveWithComponent.lastOrNull()

    // because positiveWithComponent is key-disjoint, the last added component holds exactly the keys that are new, if any
    val newUncovered =
      if (lastAdded != null && lastAdded !== positive.lastOrNull()) uncoveredKeys without lastAdded.keys
      else uncoveredKeys

    return MapComponentConjunction(positiveWithComponent, newUncovered, negative)
  }

  infix fun andNot(comp: MapComponent) = MapComponentConjunction(positive, uncoveredKeys, negative + comp)

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
    return TODO()
  }

  fun isEmpty(): Boolean {
    require(!isIrreversiblyEmpty()) { "${::isEmpty.name}() called on irreversibly empty conjunction!" } // TODO better method for checking
    if (negative.isEmpty()) return false

    val satisfier = NegativesSatisfier()
    return searchPartition { keys, positiveValues, negatives ->
      satisfier.addCandidate(keys, positiveValues, negatives)
        .ifDecided { return@searchPartition stopAndReturn(!it.isDefinitelySatisfiable()) }
      continueSearching()
    }.orElse { !satisfier.canSatisfyAllNegatives() }
  }

  private fun <R : Any> searchPartition(
    consumer: (keys: SolvableSet, positiveValues: MaybeAbsent, negatives: NegativeMapComponents) -> SearchOutcome<R>
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
    consumer: (keys: SolvableSet, positiveValues: MaybeAbsent, negatives: NegativeMapComponents) -> SearchOutcome<R>
  ): SearchOutcome<R> {
    return if (negativesIndex == negative.size) {
      consumer(keys, positiveValues, negativeMapComponents.copy())
    } else {
      val negative = negative[negativesIndex]
      val negativeKeyOverlap = keys overlapWith negative.keys
      if (negativeKeyOverlap.isEmpty()) {
        searchPartition(keys, positiveValues, negativeMapComponents, negativesIndex + 1, consumer)
      } else {
        negativeMapComponents.with(negativesIndex) { withNegative ->
          searchPartition(negativeKeyOverlap, positiveValues, withNegative, negativesIndex + 1, consumer)
        }.elseTry {
          val withoutNegativeKeys = keys without negative.keys
          if (!withoutNegativeKeys.isEmpty()) {
            searchPartition(withoutNegativeKeys, positiveValues, negativeMapComponents, negativesIndex + 1, consumer)
          } else notFound()
        }
      }
    }
  }

  @JvmInline
  internal value class KeyDisjointMapComponents private constructor(
    /**
     * notable cases:
     *   - `components == [MapComoponent.empty()]` represents the empty (that is: uninhabited) map set. All uninhabited variants are
     *      normalized to this representation.
     *   - `components == []` represents the set containing all maps.
     */
    private val components: List<MapComponent>
  ) {
    init {
      ::components.checkInvariant("contains no empty key or value types (except MapComponent.empty())") { components ->
        components.none { (it.keys.isEmpty() || it.values.isEmpty()) && it != MapComponent.empty() }
      }
    }

    operator fun plus(component: MapComponent): KeyDisjointMapComponents {
      if (component.values.isEmpty()) return empty()

      return KeyDisjointMapComponents(buildList(size + 1) {
        var toAdd: MapComponent? = component

        for (preExisting in components) {
          if (toAdd == null) {
            add(preExisting)
            continue
          }

          // no special handling for `toAdd.keys triviallyEquals preExisting.keys` because that will be handled efficiently by the following
          // code
          if (toAdd.values triviallyEquals preExisting.values) {
            add(preExisting.withKeys(preExisting.keys with toAdd.keys))
            toAdd = null
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
            add(preExisting.withKeys(preExistingKeysWithoutOverlap))
          }

          val toAddKeysWithoutOverlap = toAdd.keys without keyOverlap
          toAdd = if (!toAddKeysWithoutOverlap.isEmpty()) toAdd.withKeys(toAddKeysWithoutOverlap)
          else null
        }

        if (toAdd != null) add(toAdd)
      })
    }

    fun lastOrNull() = components.lastOrNull()

    fun isEmpty(): Boolean = components.size == 1 && components[0] == MapComponent.empty()

    operator fun iterator() = components.iterator()

    val size get() = components.size

    companion object {
      internal fun empty(): KeyDisjointMapComponents = KeyDisjointMapComponents(listOf(MapComponent.empty()))
      internal fun complete(): KeyDisjointMapComponents = KeyDisjointMapComponents(emptyList())
      internal fun just(component: MapComponent) = KeyDisjointMapComponents(listOf(component))
    }
  }


  private class NegativeMapComponents(private val contained: BooleanArray, private var _size: Int) {
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

    inline fun <R> with(negativeIndex: Int, block: (NegativeMapComponents) -> R): R {
      val previous = this[negativeIndex]
      return if (previous) block(this)
      else {
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
    inline fun removeIndexIf(predicate: (Int) -> Boolean): /* removed any? */ Boolean {
      var index = 0
      var retained = 0
      val previousSize = _size
      while (retained < _size) {
        if (contained[index]) {
          if (predicate(index)) unset(index)
          else retained += 1
        }
        index += 1
      }
      return previousSize != _size
    }

    fun iterator(negatives: List<MapComponent>): MutableIterator<MapComponent> = object : MutableIterator<MapComponent> {
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

  private operator fun NegativeMapComponents.iterator(): MutableIterator<MapComponent> = iterator(this@MapComponentConjunction.negative)

  @IgnorableReturnValue
  private inline fun NegativeMapComponents.removeIf(predicate: (MapComponent) -> Boolean): /* removed any?*/ Boolean {
    val negativeIt = iterator()
    val previousSize = size
    while (negativeIt.hasNext()) {
      if (predicate(negativeIt.next())) negativeIt.remove()
    }
    return previousSize != size
  }

  /**
   * An undecided outcome leaves candidate links and capacities consistent; pending checks may still need propagation.
   * A definitely satisfiable outcome is terminal and must be returned immediately through the calling mutations and loops.
   * It may skip candidate updates and discard pending checks: neither this state nor its worklist may be used or copied again.
   * A definitely unsatisfiable outcome likewise abandons this search attempt; alternatives use their own state copies.
   */
  private inner class NegativesSatisfier(
    // TODO consider a more efficient data structure here, especially instead of HashSet<Int> for each negative. The ArrayDeques as well.
    // A null entry means that the negative has been satisfied.
    private val candidatesPerNegative: Array<MutableSet<Int>?> = Array(negative.size) { HashSet() },
    private val candidates: MutableList<Partition?> = mutableListOf(),
    private var toSatisfy: Int = negative.size,
    private val candidatesToCheckForTriviallySatisfyingAll: ArrayDeque<Int> = ArrayDeque(),
    private val negativesToCheckForRemainingCandidates: ArrayDeque<Int> = ArrayDeque()
  ) {
    fun addCandidate(keys: SolvableSet, positiveValues: MaybeAbsent, satisfiableNegatives: NegativeMapComponents): PropagationOutcome {
      satisfiableNegatives.removeIndexIf(this::isSatisfied)
      satisfiableNegatives.removeIf {
        // The negative forbids every positive value of this partition -> we cannot satisfy it in this partition.
        // We rely on each candidate actually being able to satisfy its negatives, so isTriviallyEmpty would not be enough here.
        (positiveValues without it.values).isEmpty()
      }
      if (satisfiableNegatives.isEmpty()) return moreToSatisfy()

      val keyCardinality = keys.cardinality(satisfiableNegatives.size)
      if (keyCardinality >= satisfiableNegatives.size) {
        // We have enough keys to use a new key to satisfy each of the remaining negatives. Hence, we can satisfy them all with
        // this partition and assume eagerly that we do so. There is no need to retain alternative assignments or store this
        // partition: it has done all it could do, and its keys are disjoint from those of the other partitions.
        // This unregistered set stays unchanged during the loop.
        satisfiableNegatives.forEachIndex { negative ->
          markSatisfied(negative)
            .ifDecided { decision -> return decision }
        }
      } else {
        addCandidate(Partition(keys, positiveValues, satisfiableNegatives, keyCardinality.exactValue))
      }
      return propagateCandidates()
    }

    fun canSatisfyAllNegatives(): Boolean {
      negativesToCheckForRemainingCandidates.addAll(candidatesPerNegative.indices)
      propagateDecisions().ifDecided { return it.isDefinitelySatisfiable() }
      return tryAllCombinationsToSatisfy()
    }

    private fun tryAllCombinationsToSatisfy(minNegativeIndex: Int = 0): Boolean {
      var negativeToTry = minNegativeIndex
      var candidatesForNegative = candidatesPerNegative[negativeToTry]
      while (candidatesForNegative == null) {
        negativeToTry += 1
        candidatesForNegative = candidatesPerNegative[negativeToTry]
      }

      for (candidateToTry in candidatesForNegative) {
        val copy = copy()
        copy.satisfyAndPropagate(negativeToTry, using = candidateToTry)
          .ifDecided { if (it.isDefinitelySatisfiable()) return true else continue }
        if (copy.tryAllCombinationsToSatisfy()) return true
      }
      return false
    }

    private fun addCandidate(candidate: Partition) {
      // The caller supplies only still-open negatives: new partitions filter out satisfied negatives, and rest candidates
      // inherit the original candidate's up-to-date set. Hence, their candidatesPerNegative entries cannot be null.
      val index = candidates.size
      candidates.add(candidate)
      candidate.satisfiableNegatives.forEachIndex { negative ->
        candidatesPerNegative[negative]!!.add(index)
        negativesToCheckForRemainingCandidates.addLast(negative)
      }
      candidatesToCheckForTriviallySatisfyingAll.addLast(index)
    }

    private fun markSatisfied(negative: Int): PropagationOutcome {
      val affectedCandidates: Set<Int> = candidatesPerNegative[negative] ?: return moreToSatisfy()
      candidatesPerNegative[negative] = null
      toSatisfy -= 1
      // Success is terminal: skip the remaining updates and, in satisfyUsing, potentially expensive type operations.
      if (allNegativesSatisfied()) return definitelySatisfiable()
      // The detached set is stable: no propagation runs inside this loop.
      for (index in affectedCandidates) {
        candidates[index]!!.satisfiableNegatives.unset(negative)
        candidatesToCheckForTriviallySatisfyingAll.addLast(index)
      }
      return moreToSatisfy()
    }

    private fun removeCandidate(index: Int) {
      val candidate = candidates.set(index, null)!!
      candidate.satisfiableNegatives.forEachIndex { negative ->
        // This set cannot be null: it still contains this candidate, so the negative has not been satisfied.
        candidatesPerNegative[negative]!!.remove(index)
        negativesToCheckForRemainingCandidates.addLast(negative)
      }
    }

    private fun satisfyAndPropagate(negativeIndex: Int, using: Int) = satisfyUsing(negativeIndex, using)
      .ifUndecidedTry { propagateDecisions() }

    private fun satisfyUsing(negative: Int, candidateIndex: Int): PropagationOutcome {
      val candidate = candidates[candidateIndex]!!
      // TODO check invariant: negative is satisfiable by candidate.
      candidate.remainingKeys -= 1
      markSatisfied(negative)
        .ifDecided { decision -> return decision }

      // The key we used to satisfy the negative might also satisfy another negative. If that is possible, we need to add
      // a candidate for this possibility, with its values restricted by the negative we have already satisfied.

      // This cannot be empty, otherwise this would not be a candidate for this negative.
      // TODO check this invariant to help detect bugs in candidate registration or selection.
      val remainingValues = candidate.positiveValues without this@MapComponentConjunction.negative[negative].values
      val remainingNegatives = candidate.satisfiableNegatives.copy()
      remainingNegatives.removeIf {
        // The negative forbids every remaining positive value -> we cannot satisfy it with this restricted candidate.
        (remainingValues without it.values).isEmpty()
      }
      if (remainingNegatives.isNotEmpty()) {
        addCandidate(Partition(candidate.keys, remainingValues, remainingNegatives, 1))
      }
      // Register the replacement before checking obligations affected by depletion.
      if (candidate.remainingKeys == 0) removeCandidate(candidateIndex)
      return moreToSatisfy()
    }

    // potential optimisation: initially and every time we make a change, we can check for candidates whose positive type and whose set
    // of satisfiable negatives are both equal. We can merge those candidates, creating a larger key type that may satisfy obligations
    // trivially no or in the future.

    private fun propagateDecisions(): PropagationOutcome {
      while (candidatesToCheckForTriviallySatisfyingAll.isNotEmpty() || negativesToCheckForRemainingCandidates.isNotEmpty()) {
        propagateCandidates()
          .ifDecided { decision -> return decision }
        propagateNegatives()
          .ifDecided { decision -> return decision }
      }
      return moreToSatisfy()
    }

    private fun propagateCandidates(): PropagationOutcome {
      while (candidatesToCheckForTriviallySatisfyingAll.isNotEmpty()) {
        val index = candidatesToCheckForTriviallySatisfyingAll.removeFirst()
        val candidate = candidates[index] ?: continue
        if (candidate.canTriviallySatisfyAllNegatives()) {
          removeCandidate(index)
          candidate.satisfiableNegatives.forEachIndex { negative ->
            markSatisfied(negative)
              .ifDecided { decision -> return decision }
          }
        }
      }
      return moreToSatisfy()
    }

    private fun propagateNegatives(): PropagationOutcome {
      while (negativesToCheckForRemainingCandidates.isNotEmpty() && candidatesToCheckForTriviallySatisfyingAll.isEmpty()) {
        val negative = negativesToCheckForRemainingCandidates.removeFirst()
        val remainingCandidates = candidatesPerNegative[negative] ?: /* negative is already satisfied */ continue
        when (remainingCandidates.size) {
          0 -> {
            // remove the remaining negatives so the list is cleared for use by the next try. candidatesToCheckForTriviallySatisfyingAll
            // is empty at this point.
            negativesToCheckForRemainingCandidates.clear()
            return definitelyUnsatisfiable()
          }
          // All partitions are known, and this negative can only be satisfied by one candidate. It must take this
          // obligation, so we can make the choice eagerly without branching.
          1 -> satisfyUsing(negative, remainingCandidates.single())
            .ifDecided { decision -> return decision }
        }
      }
      return moreToSatisfy()
    }

    fun copy(): NegativesSatisfier {
      ::candidatesToCheckForTriviallySatisfyingAll.checkInvariant("is empty when copying") { it.isEmpty() }
      ::negativesToCheckForRemainingCandidates.checkInvariant("is empty when copying") { it.isEmpty() }
      return NegativesSatisfier(
        Array(candidatesPerNegative.size) { candidatesPerNegative[it]?.let(::HashSet) },
        MutableList(candidates.size) { candidates[it]?.copy() },
        toSatisfy,
        this.candidatesToCheckForTriviallySatisfyingAll,
        this.negativesToCheckForRemainingCandidates
      )
    }

    private fun isSatisfied(negativeIndex: Int) = candidatesPerNegative[negativeIndex] == null
    fun allNegativesSatisfied() = toSatisfy == 0
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
    private val state: Byte
  ) {

    inline fun ifDecided(consumer: (outcome: PropagationOutcome) -> Unit) {
      contract {
        callsInPlace(consumer, AT_MOST_ONCE)
      }
      if (state != 0.toByte()) consumer(this)
    }

    inline fun ifUndecidedTry(next: () -> PropagationOutcome): PropagationOutcome {
      contract {
        callsInPlace(next, AT_MOST_ONCE)
      }
      return if (state == 0.toByte()) next() else this
    }

    fun isDefinitelySatisfiable() = state == 2.toByte()
  }

  private fun definitelySatisfiable() = PropagationOutcome(2)
  private fun definitelyUnsatisfiable() = PropagationOutcome(1)
  private fun moreToSatisfy() = PropagationOutcome(0)

  companion object {
    private val empty = MapComponentConjunction(
      positive = KeyDisjointMapComponents.empty(),
      uncoveredKeys = TypePartitionUnion.empty(),
      negative = emptyList()
    )
    private val complete = MapComponentConjunction(
      positive = KeyDisjointMapComponents.complete(),
      uncoveredKeys = TypePartitionUnion.complete(),
      negative = emptyList()
    )

    internal fun empty() = empty
    internal fun complete() = complete
    internal fun just(component: MapComponent) = MapComponentConjunction(
      positive = KeyDisjointMapComponents.just(component),
      uncoveredKeys = TypePartitionUnion.complete() without TypePartitionUnion.empty().copy(maps = component),
      negative = emptyList()
    )
  }
}
