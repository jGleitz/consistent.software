package software.consistent.data.type.solvers.maps

import software.consistent.data.formatting.monospacelayout.Monospace
import software.consistent.data.formatting.monospacelayout.WithMonospaceLayout
import software.consistent.data.type.solvers.*
import software.consistent.data.type.solvers.Cardinality.Companion.exactly
import software.consistent.data.type.solvers.Cardinality.Companion.moreThanIntMaxValue
import software.consistent.data.type.solvers.maps.SearchOutcome.Companion.continueSearching
import software.consistent.data.value.Value

internal class MapComponent(
  val keys: SolvableSet,
  val values: MaybeAbsent
) : MapSet, LazyBdd.DecisionNode<Value.Map, MapComponent, MapSet>(), WithMonospaceLayout {
  init {
    // TODO better mechanism for checking
    require(values.includesAbsent || values.values.isTriviallyEmpty() || keys.isFinite()) {
      "A required map component must have a finite set of keys"
    }
  }

  operator fun component1() = keys
  operator fun component2() = values

  fun withKeys(newKeys: SolvableSet) = MapComponent(newKeys, values)
  fun withValues(newValues: MaybeAbsent) = MapComponent(keys, newValues)

  override val asRep: MapSet get() = this
  override val bdd: LazyBdd<Value.Map, MapComponent, MapSet> get() = this

  override val discriminator: MapComponent get() = this
  override val ifTrue: MapSet get() = MapSet.complete()
  override val regardless: MapSet get() = MapSet.empty()
  override val ifFalse: MapSet get() = MapSet.empty()

  override fun node(
    discriminator: MapComponent,
    ifTrue: MapSet,
    regardless: MapSet,
    ifFalse: MapSet
  ): MapSet = MapSet.node(discriminator, ifTrue, regardless, ifFalse)

  override fun bottom() = MapSet.empty()

  override fun top() = MapSet.complete()

  override fun anyMember(): Value.Map? {
    if (values.includesAbsent) return Value.Map()
    val value = values.values.anyMember() ?: return null
    return Value.Map(keys.members.map { it to value })
  }

  override val members: Sequence<Value.Map>
    get() = TODO("Not yet implemented")

  override fun isFinite() = cardinality(2).isExact()

  override fun cardinality(upTo: Int): Cardinality {
    if (values.values.isEmpty()) {
      // the component allows no map
      if (!values.includesAbsent) return exactly(0)
      // the component allows only the empty map
      if (keys.isComplete()) return exactly(1)
    }
    // Either the keys are infinite. Then the values aren’t required (class invariant) and there are infinitely many combinations.
    // Or the keys are finite. Then there are infinitely many combinations for all other keys, which are not constrained by this component.
    return moreThanIntMaxValue()
  }

  override fun contains(value: Value.Map): Boolean =
    if (values.includesAbsent) value.entries.all { it.key !in keys || it.value in values.values }
    else {
      var keyMatches = 0
      for ((key, value) in value.entries) {
        if (key in keys) {
          keyMatches += 1
          if (value !in values.values) return false
        }
      }
      // All keys present in value have the correct value. Now we have to check that all expected keys were present.
      // We rely on our invariant that only finite keys may be required.
      keys.cardinality(keyMatches) == exactly(keyMatches)
    }

  override fun normalizationComparedTo(other: MapSet) = when (other) {
    is MapComponent -> this normalizationComparedTo other
    is LazyBdd<*, *, *> -> (other normalizationComparedTo this).reversed()
  }

  infix fun normalizationComparedTo(other: MapComponent) =
    (this.keys normalizationComparedTo other.keys)
      .ifEqual { this.values normalizationComparedTo other.values }

  override fun isEmpty() = values.isEmpty() && !keys.isEmpty()
  override fun isTriviallyEmpty() = values.isTriviallyEmpty() && keys.isTriviallyNotEmpty()
  override fun isTriviallyNotEmpty() = keys.isTriviallyEmpty() || values.isTriviallyNotEmpty()
  override fun isComplete() = values.isComplete() || keys.isEmpty()
  override fun isTriviallyComplete() = values.isTriviallyComplete() || keys.isTriviallyEmpty()

  override fun triviallyEquals(otherSet: ClosedSolvableSet<*, *>) =
    this === otherSet || (otherSet is MapComponent && this.keys triviallyEquals otherSet.keys && this.values triviallyEquals otherSet.values)

  override fun <Result : Any> searchConjunction(consumer: (MapComponentConjunction) -> SearchOutcome<Result>) =
    if (isEmpty()) continueSearching()
    else consumer(MapComponentConjunction.just(this))

  override fun <Result : Any> searchConjunction(
    accumulator: MapComponentConjunction,
    consumer: (MapComponentConjunction) -> SearchOutcome<Result>
  ): SearchOutcome<Result> {
    val branchConjunction = accumulator and this
    return if (branchConjunction.isIrreversiblyEmpty()) continueSearching()
    else consumer(branchConjunction)
  }

  override val monospaceLayout
    get() = Monospace.line("→", 2) {
      -keys.monospaceLayout.containerWrappedIn("(", ")")
      -values.monospaceLayout.containerWrappedIn("(", ")")
    }

  override fun toString() = monospaceLayout.renderToString()

  companion object {
    private val empty: MapComponent = MapComponent(TypePartitionUnion.complete(), MaybeAbsent.empty())

    internal fun empty() = empty
  }
}
