@file:OptIn(ExperimentalContracts::class)

package software.consistent.data.type.solvers.maps

import androidx.collection.CircularIntArray
import androidx.collection.MutableIntList
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.jvm.JvmInline
import software.consistent.collections.BitSet
import software.consistent.data.invariants.checkInvariant
import software.consistent.data.type.solvers.SolvableSet
import software.consistent.data.type.solvers.TypePartitionUnion
import software.consistent.data.type.solvers.maps.MapComponentConjunction.PropagationOutcome.Companion.definitelyInhabited
import software.consistent.data.type.solvers.maps.MapComponentConjunction.PropagationOutcome.Companion.definitelyNotInhabited
import software.consistent.data.type.solvers.maps.MapComponentConjunction.PropagationOutcome.Companion.undecided
import software.consistent.data.value.Value

internal class MapComponentConjunction(
    /*
     * Semantically, these collections are sets. But the invariants of the LazyBdd and this class guarantee that there will be no duplicates.
     * Hence, we use arrays, which are cheaper to copy.
     */
    /**
     * The positive atoms of the conjunction, split such that each component’s key type is disjunct
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
    val builder = InhabitationCheckBuilder(negative, MemberPlan())
    if (negative.isNotEmpty()) {
      context(PropagationQueues(negative.size)) {
        forEachPartition { keys, values, negatives ->
          builder.addPartition(keys, values, negatives).ifDecided {
            return if (it == definitelyInhabited()) materialise(builder.plan!!) else null
          }
        }
        val search = builder.build()
        if (!search.isInhabited()) return null
        return materialise(search.plan!!)
      }
    }
    return materialise(builder.plan!!)
  }

  private fun materialise(plan: MemberPlan): Value.Map {
    val entries = mutableMapOf<Value, Value>()
    // Newest constraints win. Neither slots nor their sources contain actual keys.
    val finalValues = mutableMapOf<KeySlot, MaybeAbsent>()
    var step = plan.last
    while (step != null) {
      if (step.slot !in finalValues) finalValues[step.slot] = step.values
      step = step.previous
    }
    val iterators = mutableMapOf<KeySource, Iterator<Value>>()
    for ((slot, values) in finalValues) {
      if (values.includesAbsent) continue
      val keys = iterators.getOrPut(slot.source) { slot.source.keys.members.iterator() }
      entries[keys.next()] = checkNotNull(values.values.anyMember())
    }
    // Search may have succeeded before visiting every partition. Fill required positives from
    // the original conjunction, preserving the entries selected by the plan.
    for ((keys, values) in positive) {
      if (values.includesAbsent) continue
      val defaultValue by lazy { checkNotNull(values.values.anyMember()) }
      for (key in keys.members) {
        if (key !in entries) entries[key] = defaultValue
      }
    }
    return Value.Map(entries)
  }

  private class KeySource(val keys: SolvableSet)

  private class KeySlot(val source: KeySource)

  private class MemberStep(
      val slot: KeySlot,
      val values: MaybeAbsent,
      val previous: MemberStep?,
  )

  /** A branch-local head into an immutable trail; snapshots share the prefix in O(1). */
  private class MemberPlan(var last: MemberStep? = null) {
    fun constrain(slot: KeySlot, values: MaybeAbsent) {
      last = MemberStep(slot, values, last)
    }

    fun snapshot() = MemberPlan(last)
  }

  fun isEmpty(): Boolean {
    // TODO better method for checking
    require(!isIrreversiblyEmpty()) {
      "${::isEmpty.name}() called on irreversibly empty conjunction!"
    }
    if (negative.isEmpty()) return false

    return context(PropagationQueues(negative.size)) {
      val inhabitationCheck = InhabitationCheckBuilder(negative)
      forEachPartition { keys, positiveValues, negatives ->
        inhabitationCheck.addPartition(keys, positiveValues, negatives).ifDecided {
          return@isEmpty it == definitelyNotInhabited()
        }
      }
      inhabitationCheck.build().isNotInhabited()
    }
  }

  private inline fun forEachPartition(
      consumer: (keys: SolvableSet, positiveValues: MaybeAbsent, negatives: BitSet) -> Unit,
  ) {
    val includedNegatives = BitSet(negative.size)
    val keyMemory = arrayOfNulls<SolvableSet>(negative.size)

    for ((keys, values) in positive) {
      forEachPartition(keys, values, consumer, includedNegatives, keyMemory)

      // TODO better method for checking
      check(includedNegatives.isEmpty()) { "includedNegatives not cleared" }
      check(keyMemory.all { it == null }) { "keyMemory not cleared" }
    }

    val uncoveredKeys =
        positive.fold(TypePartitionUnion.complete()) { acc, positive -> acc without positive.keys }
    if (!uncoveredKeys.isEmpty()) {
      forEachPartition(
          uncoveredKeys,
          MaybeAbsent.complete(),
          consumer,
          includedNegatives,
          keyMemory,
      )
    }
  }

  private inline fun forEachPartition(
      keys: SolvableSet,
      positiveValues: MaybeAbsent,
      consumer: (keys: SolvableSet, positiveValues: MaybeAbsent, negatives: BitSet) -> Unit,
      includedNegatives: BitSet,
      keysBeforeIntersecting: Array<SolvableSet?>,
  ) {
    var remainingKeys = keys
    var negativeIndex = 0

    // Descend through intersections, saving the keys before each included negative.
    // After emitting a partition, undo inclusions in reverse order and resume descending
    // from the first non-empty difference.

    while (negativeIndex != NOTHING_TO_DESCEND) {
      while (negativeIndex < negative.size) {
        val negative = negative[negativeIndex]
        val negativeKeyOverlap = remainingKeys overlapWith negative.keys
        if (negativeKeyOverlap.isNotEmpty()) {
          keysBeforeIntersecting[negativeIndex] = remainingKeys
          remainingKeys = negativeKeyOverlap
          includedNegatives.add(negativeIndex)
        }
        negativeIndex += 1
      }
      // TODO make sure to remove the copy() if and when the BitSet is no longer modified or
      // retained
      consumer(remainingKeys, positiveValues, includedNegatives.copy())
      negativeIndex = NOTHING_TO_DESCEND

      while (negativeIndex == NOTHING_TO_DESCEND && includedNegatives.isNotEmpty()) {
        val previouslyIncludedIndex = includedNegatives.highest()
        includedNegatives.remove(previouslyIncludedIndex)

        val keysAtTheIndex = keysBeforeIntersecting[previouslyIncludedIndex]
        // TODO better method of checking
        check(keysAtTheIndex != null) { "included negative without saved previous keys!" }
        keysBeforeIntersecting[previouslyIncludedIndex] = null

        val withoutNegativeKeys = keysAtTheIndex without negative[previouslyIncludedIndex].keys
        if (withoutNegativeKeys.isNotEmpty()) {
          negativeIndex = previouslyIncludedIndex + 1
          remainingKeys = withoutNegativeKeys
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

  private interface CandidatePropagation {
    val candidatesToCheckForTriviallySatisfyingAll: CircularIntArray
  }

  private class PropagationQueues(negativeCount: Int) : CandidatePropagation {
    override val candidatesToCheckForTriviallySatisfyingAll = CircularIntArray()
    // The ring needs a spare slot to hold the initial batch without growing when it becomes full.
    val negativesToCheckForRemainingCandidates = CircularIntArray(negativeCount + 1)

    fun isEmpty() =
        candidatesToCheckForTriviallySatisfyingAll.isEmpty() &&
            negativesToCheckForRemainingCandidates.isEmpty()

    fun isNotEmpty() =
        candidatesToCheckForTriviallySatisfyingAll.isNotEmpty() ||
            negativesToCheckForRemainingCandidates.isNotEmpty()
  }

  /** Immutable candidate data, shared by branch copies. */
  private class Candidate(
      val keys: SolvableSet,
      val positiveValues: MaybeAbsent,
      /**
       * Non-null entries have been proved non-empty. The array is never modified after
       * construction.
       */
      val valuesWithoutNegative: Array<MaybeAbsent?>,
      val source: KeySource? = null,
      val slot: KeySlot? = null,
  )

  /**
   * Each negative component must be violated at some key, possibly by leaving that key absent. A
   * candidate represents interchangeable keys with common allowed values. Satisfying a negative
   * takes one key from the candidate and narrows that key's allowed values to violate the
   * component. A one-key successor may satisfy further negatives using the same key; the source
   * retains any unused keys with their original allowed values.
   *
   * The builder collects partitions and propagates choices that need no alternatives. Only after
   * collection can zero candidates for a negative prove failure or a sole candidate force a choice.
   * The two eligibility indexes record the remaining possible assignments in opposite directions.
   */
  private abstract class InhabitationCheckState(
      protected val negatives: List<MapComponent>,
      // Shared by search branches. Appended candidates are truncated on backtracking.
      // Removing a candidate only clears its branch-local eligibility.
      // Null entries are candidates discarded during building, before the list is shared.
      protected val candidates: MutableList<Candidate?> = mutableListOf(),
      // A null entry means that the negative has been satisfied.
      protected val candidatesPerNegative: Array<BitSet?> = Array(negatives.size) { BitSet() },
      // A bit means that the candidate can and may satisfy this negative in the current branch.
      // Missing bits include assignments ruled out by the search and negatives satisfied elsewhere.
      // A null entry denotes a removed candidate; the list size is the branch's checkpoint.
      // optimisation potential: This might be one large “rectangular” BitSet. Depending on the
      // state size, this would be smaller. More importantly, it might be much cheaper to copy. The
      // representation complicates other operations, though, so the impact should be measured.
      protected val eligibleNegativesPerCandidate: MutableList<BitSet?> = mutableListOf(),
      protected var leftToSatisfy: Int = negatives.size,
      var plan: MemberPlan? = null,
  ) {
    protected abstract fun remainingKeys(candidate: Int): Int

    /**
     * Removes negatives with empty differences from [eligibleNegatives] and returns the
     * differences.
     */
    protected fun computeDifferencesAndFilterNegatives(
        positiveValues: MaybeAbsent,
        eligibleNegatives: BitSet,
    ): Array<MaybeAbsent?> {
      val differences = arrayOfNulls<MaybeAbsent>(negatives.size)
      eligibleNegatives.forEach { negative ->
        val difference = positiveValues without negatives[negative].values
        // Each registered candidate must actually be able to satisfy its eligible negatives:
        // isTriviallyEmpty would not be enough here. Retain both the type and this knowledge.
        if (difference.isEmpty()) {
          eligibleNegatives.remove(negative)
        } else {
          differences[negative] = difference
        }
      }
      return differences
    }

    /**
     * Registers both directions of eligibility and schedules candidate propagation. During search,
     * callers must also schedule checks for the affected negatives.
     */
    context(propagation: CandidatePropagation)
    protected fun addCandidate(candidate: Candidate, eligibleNegatives: BitSet) {
      val index = candidates.size
      candidates.add(candidate)
      this.eligibleNegativesPerCandidate.add(eligibleNegatives)
      eligibleNegatives.forEach { negative ->
        candidatesPerNegative[negative]!!.add(index)
      }
      propagation.candidatesToCheckForTriviallySatisfyingAll.addLast(index)
    }

    context(propagation: CandidatePropagation)
    protected fun markSatisfied(negative: Int): PropagationOutcome {
      val affectedCandidates = candidatesPerNegative[negative] ?: return undecided()
      candidatesPerNegative[negative] = null
      leftToSatisfy -= 1
      // Success is terminal: skip the remaining updates and potentially expensive type operations.
      if (leftToSatisfy == 0) return definitelyInhabited()
      // The detached set is stable: no propagation runs inside this loop.
      affectedCandidates.forEach { index ->
        eligibleNegativesPerCandidate[index]!! -= negative
        propagation.candidatesToCheckForTriviallySatisfyingAll.addLast(index)
      }
      return undecided()
    }

    /**
     * Removes both directions of eligibility and returns the candidate's former eligible negatives.
     * Callers must schedule rechecks for any of these negatives they leave unsatisfied.
     */
    protected open fun removeCandidate(index: Int): BitSet {
      val eligibleNegatives = this.eligibleNegativesPerCandidate.set(index, null)!!
      eligibleNegatives.forEach { negative ->
        // This negative is not satisfied: it still contains this candidate.
        candidatesPerNegative[negative]!!.remove(index)
      }
      return eligibleNegatives
    }

    context(propagation: CandidatePropagation)
    protected fun propagateCandidates(): PropagationOutcome {
      while (propagation.candidatesToCheckForTriviallySatisfyingAll.isNotEmpty()) {
        val index = propagation.candidatesToCheckForTriviallySatisfyingAll.popFirst()
        val eligibleNegatives = this.eligibleNegativesPerCandidate[index] ?: continue
        // We have enough keys to use a distinct key for each remaining negative.
        if (remainingKeys(index) >= eligibleNegatives.size) {
          // Every affected negative is satisfied below; no negative needs a candidate-count check.
          val candidate = candidates[index]!!
          val eligible = removeCandidate(index)
          eligible.forEach { negative ->
            plan?.constrain(
                candidate.slot ?: KeySlot(candidate.source!!),
                candidate.valuesWithoutNegative[negative]!!,
            )
            markSatisfied(negative).ifDecided { decision ->
              return decision
            }
          }
        }
      }
      return undecided()
    }

    protected fun isSatisfied(negativeIndex: Int) = candidatesPerNegative[negativeIndex] == null
  }

  private class InhabitationCheckBuilder(negatives: List<MapComponent>, plan: MemberPlan? = null) :
      InhabitationCheckState(negatives, plan = plan) {
    private val keyCounts = MutableIntList()
    private var built = false

    override fun remainingKeys(candidate: Int) = keyCounts[candidate]

    override fun removeCandidate(index: Int): BitSet {
      // No search snapshots exist yet, so discarded candidate data can still be released eagerly.
      candidates[index] = null
      return super.removeCandidate(index)
    }

    context(propagation: CandidatePropagation)
    fun addPartition(
        keys: SolvableSet,
        positiveValues: MaybeAbsent,
        eligibleNegatives: BitSet,
    ): PropagationOutcome {
      check(!built) { "Cannot add partitions after building the search" }
      eligibleNegatives.removeAll(this::isSatisfied)
      if (eligibleNegatives.isEmpty()) return undecided()
      val differences = computeDifferencesAndFilterNegatives(positiveValues, eligibleNegatives)
      if (eligibleNegatives.isEmpty()) return undecided()

      val keyCardinality = keys.cardinality(eligibleNegatives.size)
      if (keyCardinality >= eligibleNegatives.size) {
        // This partition can satisfy every eligible negative using distinct keys. Its keys are
        // disjoint from the other partitions, so no alternative assignments need to be retained.
        val source = plan?.let { KeySource(keys) }
        eligibleNegatives.forEach { negative ->
          plan?.constrain(KeySlot(source!!), differences[negative]!!)
          markSatisfied(negative).ifDecided { decision ->
            return decision
          }
        }
      } else {
        keyCounts.add(keyCardinality.exactValue)
        addCandidate(
            Candidate(keys, positiveValues, differences, plan?.let { KeySource(keys) }),
            eligibleNegatives,
        )
      }
      return propagateCandidates()
    }

    fun build(): InhabitationCheck {
      check(!built) { "Search already built" }
      built = true
      // Transfer ownership of the mutable candidate tables. The builder cannot be used afterwards.
      return InhabitationCheck(
          negatives,
          candidates,
          candidatesPerNegative,
          eligibleNegativesPerCandidate,
          leftToSatisfy,
          IntArray(keyCounts.size) { keyCounts[it] },
          plan,
      )
    }
  }

  private class InhabitationCheck(
      negatives: List<MapComponent>,
      candidates: MutableList<Candidate?>,
      candidatesPerNegative: Array<BitSet?>,
      eligibleNegativesPerCandidate: MutableList<BitSet?>,
      leftToSatisfy: Int,
      // Only candidates added through the builder need stored counts. Candidates added by this
      // class have only one key until they are removed. Merging candidates would require revisiting
      // this rule.
      private val remainingKeysPerCandidate: IntArray,
      plan: MemberPlan?,
  ) :
      InhabitationCheckState(
          negatives,
          candidates,
          candidatesPerNegative,
          eligibleNegativesPerCandidate,
          leftToSatisfy,
          plan,
      ) {
    override fun remainingKeys(candidate: Int) =
        if (candidate < remainingKeysPerCandidate.size) remainingKeysPerCandidate[candidate] else 1

    /** Returns whether consuming a key exhausts this candidate. */
    private fun consumeKey(candidate: Int) =
        candidate >= remainingKeysPerCandidate.size || --remainingKeysPerCandidate[candidate] == 0

    context(propagation: PropagationQueues)
    fun isNotInhabited() = !isInhabited()

    context(propagation: PropagationQueues)
    fun isInhabited(): Boolean {
      for (negative in negatives.indices) {
        if (isSatisfied(negative)) continue
        propagation.negativesToCheckForRemainingCandidates.addLast(negative)
      }
      propagateDecisions().ifDecided {
        return it == definitelyInhabited()
      }
      return isInhabitedBySearch()
    }

    context(propagation: PropagationQueues)
    private fun isInhabitedBySearch(): Boolean {
      val toRetry = ArrayDeque<InhabitationCheck>()
      var branch = this
      var outcome = undecided()

      while (outcome != definitelyNotInhabited() || toRetry.isNotEmpty()) {
        if (outcome != definitelyNotInhabited()) {
          branch.pickNextTry { negative, candidate ->
            // Save a retry snapshot before committing this choice to the current branch.
            toRetry.addLast(branch.snapshotForRetry())

            outcome = branch.satisfyAndPropagate(negative, using = candidate)
          }
        } else {
          branch = toRetry.removeLast()
          outcome = branch.restoreForRetry()
        }
        if (outcome == definitelyInhabited()) {
          plan = branch.plan
          return true
        }
      }

      return false
    }

    private inline fun pickNextTry(consumer: (negative: Int, candidate: Int) -> Unit) {
      contract {
        callsInPlace(consumer, EXACTLY_ONCE)
      }

      var negative = -1
      var candidates: BitSet?
      do {
        negative += 1
        candidates = candidatesPerNegative[negative]
      } while (candidates == null)

      consumer(negative, candidates.lowest())
    }

    context(propagation: PropagationQueues)
    private fun satisfyAndPropagate(negativeIndex: Int, using: Int) =
        satisfyUsing(negativeIndex, using).ifUndecidedTry { propagateDecisions() }

    context(propagation: PropagationQueues)
    private fun satisfyUsing(negative: Int, candidateIndex: Int): PropagationOutcome {
      val candidate = candidates[candidateIndex]!!
      val exhausted = consumeKey(candidateIndex)
      // Registration already proved this difference non-empty.
      val remainingValues = candidate.valuesWithoutNegative[negative]!!
      val slot = plan?.let { candidate.slot ?: KeySlot(candidate.source!!) }
      if (slot != null) plan!!.constrain(slot, remainingValues)
      markSatisfied(negative).ifDecided { decision ->
        return decision
      }

      addSingleKeyContinuation(candidateIndex, remainingValues, slot)
      if (exhausted) {
        removeCandidate(candidateIndex).forEach { negative ->
          propagation.negativesToCheckForRemainingCandidates.addLast(negative)
        }
      }
      return undecided()
    }

    /** Keeps the key just used available for further negatives, with its narrowed value type. */
    context(propagation: PropagationQueues)
    private fun addSingleKeyContinuation(
        sourceIndex: Int,
        remainingValues: MaybeAbsent,
        slot: KeySlot?,
    ) {
      val remainingNegatives = eligibleNegativesPerCandidate[sourceIndex]!!.copy()
      if (remainingNegatives.isEmpty()) return
      val differences = computeDifferencesAndFilterNegatives(remainingValues, remainingNegatives)
      if (remainingNegatives.isEmpty()) return

      val source = candidates[sourceIndex]!!
      addCandidate(
          Candidate(source.keys, remainingValues, differences, source.source, slot),
          remainingNegatives,
      )
      remainingNegatives.forEach { negative ->
        propagation.negativesToCheckForRemainingCandidates.addLast(negative)
      }
    }

    context(propagation: PropagationQueues)
    private fun markImpossibleAndPropagate(negative: Int, candidateIndex: Int): PropagationOutcome {
      eligibleNegativesPerCandidate[candidateIndex]!!.remove(negative)
      candidatesPerNegative[negative]!!.remove(candidateIndex)

      propagation.candidatesToCheckForTriviallySatisfyingAll.addLast(candidateIndex)
      propagation.negativesToCheckForRemainingCandidates.addLast(negative)
      return propagateDecisions()
    }

    context(propagation: PropagationQueues)
    private fun propagateDecisions(): PropagationOutcome {
      while (propagation.isNotEmpty()) {
        propagateCandidates().ifDecided { decision ->
          return decision
        }
        propagateNegatives().ifDecided { decision ->
          return decision
        }
      }
      return undecided()
    }

    context(propagation: PropagationQueues)
    private fun propagateNegatives(): PropagationOutcome {
      while (
          propagation.negativesToCheckForRemainingCandidates.isNotEmpty() &&
              propagation.candidatesToCheckForTriviallySatisfyingAll.isEmpty()
      ) {
        val negative = propagation.negativesToCheckForRemainingCandidates.popFirst()
        val remainingCandidates =
            candidatesPerNegative[negative] ?: /* negative is already satisfied */ continue
        when (remainingCandidates.size) {
          0 -> {
            // remove the remaining negatives so the list is cleared for use by the next try.
            // propagation.candidatesToCheckForTriviallySatisfyingAll is empty at this point.
            propagation.negativesToCheckForRemainingCandidates.clear()
            return definitelyNotInhabited()
          }
          // All partitions are known, and this negative can only be satisfied by one candidate. It
          // must take this obligation, so we can make the choice eagerly without branching.
          1 ->
              satisfyUsing(negative, remainingCandidates.single()).ifDecided { decision ->
                return decision
              }
        }
      }
      return undecided()
    }

    /** Shares candidate storage; its checkpoint must be restored before retrying this snapshot. */
    private fun snapshotForRetry() =
        InhabitationCheck(
            negatives,
            candidates,
            Array(candidatesPerNegative.size) { candidatesPerNegative[it]?.copy() },
            MutableList(eligibleNegativesPerCandidate.size) {
              eligibleNegativesPerCandidate[it]?.copy()
            },
            this@InhabitationCheck.leftToSatisfy,
            remainingKeysPerCandidate.copyOf(),
            plan?.snapshot(),
        )

    /**
     * Restores this snapshot, excludes the failed choice and propagates the consequences. LIFO
     * retries ensure older snapshots cannot reference the discarded candidate suffix.
     */
    context(propagation: PropagationQueues)
    private fun restoreForRetry(): PropagationOutcome {
      // TODO better method for checking
      check(propagation.isEmpty()) {
        "Propagation queues must be empty before restoring a snapshot"
      }
      while (candidates.size > eligibleNegativesPerCandidate.size) {
        candidates.removeAt(candidates.lastIndex)
      }
      // Stable selection recovers the choice made immediately after taking this snapshot.
      pickNextTry { negative, candidate ->
        return markImpossibleAndPropagate(negative, candidate)
      }
    }
  }

  @JvmInline
  private value class PropagationOutcome(
      // 0 -> undecided
      // 1 -> not inhabited
      // 2 -> inhabited
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

    fun isDefinitelyNotInhabited() = state == 1.toByte()

    companion object {
      fun definitelyInhabited() = PropagationOutcome(2)

      fun definitelyNotInhabited() = PropagationOutcome(1)

      fun undecided() = PropagationOutcome(0)
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

private const val NOTHING_TO_DESCEND = -1
