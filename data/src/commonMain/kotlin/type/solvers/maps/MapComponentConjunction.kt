@file:OptIn(ExperimentalContracts::class)

package software.consistent.data.type.solvers.maps

import androidx.collection.CircularIntArray
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.jvm.JvmInline
import software.consistent.collections.BitSet
import software.consistent.data.invariants.checkInvariant
import software.consistent.data.type.solvers.SolvableSet
import software.consistent.data.type.solvers.TypePartitionUnion
import software.consistent.data.type.solvers.maps.MapComponentConjunction.PropagationOutcome.Companion.definitelySatisfiable
import software.consistent.data.type.solvers.maps.MapComponentConjunction.PropagationOutcome.Companion.definitelyUnsatisfiable
import software.consistent.data.type.solvers.maps.MapComponentConjunction.PropagationOutcome.Companion.moreToSatisfy
import software.consistent.data.type.solvers.maps.SearchOutcome.Companion.continueSearching
import software.consistent.data.type.solvers.maps.SearchOutcome.Companion.notFound
import software.consistent.data.type.solvers.maps.SearchOutcome.Companion.stopAndReturn
import software.consistent.data.value.Value

internal class MapComponentConjunction(
    /*
     * Semantically, these collections are sets. But the invariants of the LazyBdd and this class guarantee that there will be no duplicates.
     * Hence, we use arrays, which are cheaper to copy.
     */
    /**
     * The positive atoms of the conjunction, split such that each component’s key type in disjunct
     * from the key type of any other component.
     */
    val positive: KeyDisjointPositiveConjunction,
    /** The negative atoms of the conjunction. Their key types may overlap. */
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
  infix fun and(component: MapComponent): MapComponentConjunction =
      MapComponentConjunction(positive and component, negative)

  infix fun andNot(component: MapComponent) =
      MapComponentConjunction(positive, negative + component)

  /**
   * Return `true` iff this conjunction is known to always be empty, regardless of which components
   * are added to it with [and] or [andNot]. This is the case, for example, when the positive atoms
   * specify disjunct value types for a required key.
   *
   * If this method returns `false` the conjunction may still be empty.
   */
  fun isIrreversiblyEmpty() = positive.isEmpty()

  fun anyMember(): Value.Map? {
    // TODO better method for checking
    require(!isIrreversiblyEmpty()) {
      "${::anyMember.name}() called on irreversibly empty conjunction!"
    }
    // A member needs to come from `⋃positive ∖ ⋃negative`. We know at this point that ⋃positive is
    // not empty. Since its keys are disjunct, this means they allow at least one member. We check
    // the negative clauses progressively so we can abort the search as soon as we discover that
    // they cancel out each positive member.
    return TODO()
  }

  fun isEmpty(): Boolean {
    // TODO better method for checking
    require(!isIrreversiblyEmpty()) {
      "${::isEmpty.name}() called on irreversibly empty conjunction!"
    }
    if (negative.isEmpty()) return false

    val satisfier = NegativesSatisfier(negative)
    return searchPartition { keys, positiveValues, negatives ->
      satisfier.addCandidate(keys, positiveValues, negatives).ifDecided {
        return@searchPartition stopAndReturn(!it.isDefinitelySatisfiable())
      }
      continueSearching()
    }
        .orElse { !satisfier.canSatisfyAllNegatives() }
  }

  private fun <R : Any> searchPartition(
      consumer:
          (keys: SolvableSet, positiveValues: MaybeAbsent, negatives: BitSet) -> SearchOutcome<R>,
  ): SearchOutcome<R> {
    var outcome: SearchOutcome<R> = notFound()
    for ((keys, values) in positive) {
      outcome = searchPartition(keys, values, BitSet(negative.size), 0, consumer)
      if (outcome.isFound()) return outcome
    }

    val uncoveredKeys =
        positive.fold(TypePartitionUnion.complete()) { acc, positive -> acc without positive.keys }
    if (!uncoveredKeys.isEmpty()) {
      outcome =
          searchPartition(uncoveredKeys, MaybeAbsent.complete(), BitSet(negative.size), 0, consumer)
    }
    return outcome
  }

  private fun <R : Any> searchPartition(
      keys: SolvableSet,
      positiveValues: MaybeAbsent,
      negativeMapComponents: BitSet,
      negativesIndex: Int,
      consumer:
          (keys: SolvableSet, positiveValues: MaybeAbsent, negatives: BitSet) -> SearchOutcome<R>,
  ): SearchOutcome<R> {
    return if (negativesIndex == negative.size) {
      consumer(keys, positiveValues, negativeMapComponents.copy())
    } else {
      val negative = negative[negativesIndex]
      val negativeKeyOverlap = keys overlapWith negative.keys
      if (negativeKeyOverlap.isEmpty()) {
        searchPartition(keys, positiveValues, negativeMapComponents, negativesIndex + 1, consumer)
      } else {
        negativeMapComponents
            .with(negativesIndex) { withNegative ->
              searchPartition(
                  negativeKeyOverlap,
                  positiveValues,
                  withNegative,
                  negativesIndex + 1,
                  consumer,
              )
            }
            .elseTry {
              val withoutNegativeKeys = keys without negative.keys
              if (!withoutNegativeKeys.isEmpty()) {
                searchPartition(
                    withoutNegativeKeys,
                    positiveValues,
                    negativeMapComponents,
                    negativesIndex + 1,
                    consumer,
                )
              } else notFound()
            }
      }
    }
  }

  @JvmInline
  internal value class KeyDisjointPositiveConjunction
  private constructor(
      /**
       * notable cases:
       * - `components == [MapComoponent.empty()]` represents the empty (that is: uninhabited) map
       *   set. All uninhabited variants are normalized to this representation.
       * - `components == []` represents the set containing all maps.
       */
      private val components: List<MapComponent>,
  ) : Iterable<MapComponent> by components {
    init {
      ::components.checkInvariant(
          "contains no empty key or value types (except MapComponent.empty())",
      ) { components ->
        components.none { (it.keys.isEmpty() || it.values.isEmpty()) && it != MapComponent.empty() }
      }
    }

    infix fun and(component: MapComponent): KeyDisjointPositiveConjunction {
      // The BDD smart constructor only checks for the trivial cases. We now want a guarantee.
      // optimisation potential: These two checks potentially call component.keys.isEmpty() twice.
      if (component.isEmpty()) return empty()
      if (component.isComplete()) return this

      return KeyDisjointPositiveConjunction(
          buildList(size + 1) {
            var keysToAdd: SolvableSet? = component.keys
            val valuesToAdd: MaybeAbsent = component.values
            var sameValuesIndex = -1

            for (preExisting in components) {
              if (keysToAdd == null) {
                add(preExisting)
                continue
              }

              if (preExisting.values triviallyEquals valuesToAdd) {
                if (sameValuesIndex == -1) {
                  sameValuesIndex = size
                }
                add(preExisting)
                keysToAdd = (keysToAdd without preExisting.keys).takeIf { !it.isEmpty() }
                continue
              }

              val keyOverlap = preExisting.keys overlapWith keysToAdd
              if (keyOverlap.isEmpty()) {
                add(preExisting)
                continue
              }

              val valueOverlap = preExisting.values overlapWith valuesToAdd
              if (valueOverlap.isEmpty()) {
                // if any key is required but the effective value type is empty, the whole map type
                // is empty
                return@and empty()
              }
              add(MapComponent(keyOverlap, valueOverlap))

              val preExistingKeysWithoutOverlap = preExisting.keys without keyOverlap
              if (!preExistingKeysWithoutOverlap.isEmpty()) {
                add(preExisting.withKeys(preExistingKeysWithoutOverlap))
              }

              keysToAdd = (keysToAdd without keyOverlap).takeIf { !it.isEmpty() }
            }

            if (keysToAdd != null) {
              if (sameValuesIndex != -1) {
                val sameValuesComponent = this[sameValuesIndex]
                this[sameValuesIndex] =
                    sameValuesComponent.withKeys(sameValuesComponent.keys with keysToAdd)
              } else {
                add(MapComponent(keysToAdd, valuesToAdd))
              }
            }
          },
      )
    }

    fun isEmpty(): Boolean = components.size == 1 && components[0] == MapComponent.empty()

    val size
      get() = components.size

    companion object {
      internal fun empty(): KeyDisjointPositiveConjunction =
          KeyDisjointPositiveConjunction(listOf(MapComponent.empty()))

      internal fun complete(): KeyDisjointPositiveConjunction =
          KeyDisjointPositiveConjunction(emptyList())

      internal fun just(component: MapComponent) = KeyDisjointPositiveConjunction(listOf(component))
    }
  }

  inline fun <R> BitSet.with(element: Int, block: (BitSet) -> R): R {
    val previous = element in this
    return if (previous) block(this)
    else {
      this += element
      block(this).also { this -= element }
    }
  }

  /**
   * An undecided outcome leaves candidate links and capacities consistent; pending checks may still
   * need propagation. A definitely satisfiable outcome is terminal and must be returned immediately
   * through the calling mutations and loops. It may skip candidate updates and discard pending
   * checks: neither this state nor its worklist may be used or copied again. A definitely
   * unsatisfiable outcome likewise abandons this search attempt; alternatives use their own state
   * copies.
   */
  private class NegativesSatisfier(
      private val negatives: List<MapComponent>,
      // A null entry means that the negative has been satisfied.
      private val candidatesPerNegative: Array<BitSet?> = Array(negatives.size) { BitSet() },
      private val candidates: MutableList<Candidate?> = mutableListOf(),
      private var toSatisfy: Int = negatives.size,
      private val candidatesToCheckForTriviallySatisfyingAll: CircularIntArray = CircularIntArray(),
      private val negativesToCheckForRemainingCandidates: CircularIntArray = CircularIntArray(),
  ) {
    fun addCandidate(
        keys: SolvableSet,
        positiveValues: MaybeAbsent,
        satisfiableNegatives: BitSet,
    ): PropagationOutcome {
      satisfiableNegatives.removeAll(this::isSatisfied)
      satisfiableNegatives.removeAll {
        // The negative forbids every positive value of this partition -> we cannot satisfy it in
        // this partition. We rely on each candidate actually being able to satisfy its negatives,
        // so isTriviallyEmpty would not be enough here.
        (positiveValues without negatives[it].values).isEmpty()
      }
      if (satisfiableNegatives.isEmpty()) return moreToSatisfy()

      val keyCardinality = keys.cardinality(satisfiableNegatives.size)
      if (keyCardinality >= satisfiableNegatives.size) {
        /*
         * We have enough keys to use a new key to satisfy each of the remaining negatives. Hence,
         * we can satisfy them all with this partition and assume eagerly that we do so. There is no need to retain alternative assignments or store this partition: it has done all it could do, and its keys are disjoint from those of the other partitions.
         */
        satisfiableNegatives.forEach { negative ->
          markSatisfied(negative).ifDecided { decision ->
            return decision
          }
        }
      } else {
        addCandidate(
            Candidate(keys, positiveValues, satisfiableNegatives, keyCardinality.exactValue),
        )
      }
      return propagateCandidates()
    }

    fun canSatisfyAllNegatives(): Boolean {
      for (negative in negatives.indices) {
        negativesToCheckForRemainingCandidates.addLast(negative)
      }
      propagateDecisions().ifDecided {
        return it.isDefinitelySatisfiable()
      }
      return tryAllCombinationsToSatisfy()
    }

    private fun tryAllCombinationsToSatisfy(minNegativeIndex: Int = 0): Boolean {
      var negativeToTry = minNegativeIndex
      var candidatesForNegative: BitSet? = candidatesPerNegative[negativeToTry]
      while (candidatesForNegative == null) {
        negativeToTry += 1
        candidatesForNegative = candidatesPerNegative[negativeToTry]
      }

      // Each try works on a copy, so this set stays unchanged during the loop.
      candidatesForNegative.forEach { candidateToTry ->
        val copy = copy()
        copy.satisfyAndPropagate(negativeToTry, using = candidateToTry).ifDecided {
          if (it.isDefinitelySatisfiable()) return@tryAllCombinationsToSatisfy true
          else return@forEach
        }
        if (copy.tryAllCombinationsToSatisfy()) return true
      }
      return false
    }

    private fun addCandidate(candidate: Candidate) {
      // The caller supplies only still-open negatives: new partitions filter out satisfied
      // negatives, and rest candidates inherit the original candidate's up-to-date set. Hence,
      // their candidatesPerNegative entries cannot be null.
      val index = candidates.size
      candidates.add(candidate)
      candidate.satisfiableNegatives.forEach { negative ->
        candidatesPerNegative[negative]!!.add(index)
        negativesToCheckForRemainingCandidates.addLast(negative)
      }
      candidatesToCheckForTriviallySatisfyingAll.addLast(index)
    }

    private fun markSatisfied(negative: Int): PropagationOutcome {
      val affectedCandidates: BitSet = candidatesPerNegative[negative] ?: return moreToSatisfy()
      candidatesPerNegative[negative] = null
      toSatisfy -= 1
      // Success is terminal: skip the remaining updates and, in satisfyUsing, potentially expensive
      // type operations.
      if (allNegativesSatisfied()) return definitelySatisfiable()
      // The detached set is stable: no propagation runs inside this loop.
      affectedCandidates.forEach { index ->
        candidates[index]!!.satisfiableNegatives -= negative
        candidatesToCheckForTriviallySatisfyingAll.addLast(index)
      }
      return moreToSatisfy()
    }

    private fun removeCandidate(index: Int) {
      val candidate = candidates.set(index, null)!!
      candidate.satisfiableNegatives.forEach { negative ->
        // This set cannot be null: it still contains this candidate, so the negative has not been
        // satisfied.
        candidatesPerNegative[negative]!!.remove(index)
        negativesToCheckForRemainingCandidates.addLast(negative)
      }
    }

    private fun satisfyAndPropagate(negativeIndex: Int, using: Int) =
        satisfyUsing(negativeIndex, using).ifUndecidedTry { propagateDecisions() }

    private fun satisfyUsing(negative: Int, candidateIndex: Int): PropagationOutcome {
      val candidate = candidates[candidateIndex]!!
      // TODO check invariant: negative is satisfiable by candidate.
      candidate.remainingKeys -= 1
      markSatisfied(negative).ifDecided { decision ->
        return decision
      }

      // The key we used to satisfy the negative might also satisfy another negative. If that is
      // possible, we need to add a candidate for this possibility, with its values restricted by
      // the negative we have already satisfied.

      // This cannot be empty, otherwise this would not be a candidate for this negative.
      // TODO check this invariant to help detect bugs in candidate registration or selection.
      val remainingValues = candidate.positiveValues without negatives[negative].values
      val remainingNegatives = candidate.satisfiableNegatives.copy()
      remainingNegatives.removeAll {
        // The negative forbids every remaining positive value -> we cannot satisfy it with this
        // restricted candidate.
        (remainingValues without negatives[it].values).isEmpty()
      }
      if (remainingNegatives.isNotEmpty()) {
        addCandidate(Candidate(candidate.keys, remainingValues, remainingNegatives, 1))
      }
      if (candidate.remainingKeys == 0) removeCandidate(candidateIndex)
      return moreToSatisfy()
    }

    // potential optimisation: initially and every time we make a change, we can check for
    // candidates whose positive type and whose set of satisfiable negatives are both equal. We can
    // merge those candidates, creating a larger key type that may satisfy obligations trivially now
    // or in the future.
    private fun propagateDecisions(): PropagationOutcome {
      while (
          candidatesToCheckForTriviallySatisfyingAll.isNotEmpty() ||
              negativesToCheckForRemainingCandidates.isNotEmpty()
      ) {
        propagateCandidates().ifDecided { decision ->
          return decision
        }
        propagateNegatives().ifDecided { decision ->
          return decision
        }
      }
      return moreToSatisfy()
    }

    private fun propagateCandidates(): PropagationOutcome {
      while (candidatesToCheckForTriviallySatisfyingAll.isNotEmpty()) {
        val index = candidatesToCheckForTriviallySatisfyingAll.popFirst()
        val candidate = candidates[index] ?: continue
        if (candidate.canTriviallySatisfyAllNegatives()) {
          removeCandidate(index)
          candidate.satisfiableNegatives.forEach { negative ->
            markSatisfied(negative).ifDecided { decision ->
              return decision
            }
          }
        }
      }
      return moreToSatisfy()
    }

    private fun propagateNegatives(): PropagationOutcome {
      while (
          negativesToCheckForRemainingCandidates.isNotEmpty() &&
              candidatesToCheckForTriviallySatisfyingAll.isEmpty()
      ) {
        val negative = negativesToCheckForRemainingCandidates.popFirst()
        val remainingCandidates =
            candidatesPerNegative[negative] ?: /* negative is already satisfied */ continue
        when (remainingCandidates.size) {
          0 -> {
            // remove the remaining negatives so the list is cleared for use by the next try.
            // candidatesToCheckForTriviallySatisfyingAll is empty at this point.
            negativesToCheckForRemainingCandidates.clear()
            return definitelyUnsatisfiable()
          }
          // All partitions are known, and this negative can only be satisfied by one candidate. It
          // must take this obligation, so we can make the choice eagerly without branching.
          1 ->
              satisfyUsing(negative, remainingCandidates.single()).ifDecided { decision ->
                return decision
              }
        }
      }
      return moreToSatisfy()
    }

    fun copy(): NegativesSatisfier {
      ::candidatesToCheckForTriviallySatisfyingAll.checkInvariant("is empty when copying") {
        it.isEmpty()
      }
      ::negativesToCheckForRemainingCandidates.checkInvariant("is empty when copying") {
        it.isEmpty()
      }
      return NegativesSatisfier(
          negatives,
          Array(candidatesPerNegative.size) { candidatesPerNegative[it]?.copy() },
          MutableList(candidates.size) { candidates[it]?.copy() },
          toSatisfy,
          this.candidatesToCheckForTriviallySatisfyingAll,
          this.negativesToCheckForRemainingCandidates,
      )
    }

    private fun isSatisfied(negativeIndex: Int) = candidatesPerNegative[negativeIndex] == null

    fun allNegativesSatisfied() = toSatisfy == 0

    private class Candidate(
        val keys: SolvableSet,
        val positiveValues: MaybeAbsent,
        val satisfiableNegatives: BitSet,
        var remainingKeys: Int,
    ) {
      // We have enough keys to use a distinct key to satisfy each of the remaining negatives.
      fun canTriviallySatisfyAllNegatives() = remainingKeys >= satisfiableNegatives.size

      fun copy(): Candidate =
          Candidate(keys, positiveValues, satisfiableNegatives.copy(), remainingKeys)
    }
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

    inline fun ifUndecidedTry(next: () -> PropagationOutcome): PropagationOutcome {
      contract {
        callsInPlace(next, AT_MOST_ONCE)
      }
      return if (state == 0.toByte()) next() else this
    }

    fun isDefinitelySatisfiable() = state == 2.toByte()

    companion object {
      fun definitelySatisfiable() = PropagationOutcome(2)

      fun definitelyUnsatisfiable() = PropagationOutcome(1)

      fun moreToSatisfy() = PropagationOutcome(0)
    }
  }

  companion object {
    private val empty =
        MapComponentConjunction(
            positive = KeyDisjointPositiveConjunction.empty(),
            negative = emptyList(),
        )
    private val complete =
        MapComponentConjunction(
            positive = KeyDisjointPositiveConjunction.complete(),
            negative = emptyList(),
        )

    internal fun empty() = empty

    internal fun complete() = complete

    internal fun just(component: MapComponent) =
        MapComponentConjunction(
            positive = KeyDisjointPositiveConjunction.just(component),
            negative = emptyList(),
        )

    private fun CircularIntArray.isNotEmpty() = !isEmpty()
  }
}
