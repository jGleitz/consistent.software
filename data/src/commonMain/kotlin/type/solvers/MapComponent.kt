package software.consistent.model.type.solvers

import software.consistent.model.value.Value

internal data class MapComponent(
  val keys: SolvableSet,
  val values: MaybeAbsent,
) : ValueSet.NormalizationComparable<Value.Map, MapComponent> {
  init {
    require(values.includesAbsent ||keys.isFinite() || values.values.isEmpty()) { // TODO better mechanism for checking
      "A required map component must have a finite set of keys"
    }
  }

  override fun contains(value: Value.Map) =
    if (values.includesAbsent) value.entries.all { it.key !in keys || it.value in values.values }
    else keys.members.all { key -> value[key]?.let { it in values.values } ?: false }

  override fun normalizationComparedTo(other: MapComponent) =
    (this.keys normalizationComparedTo other.keys)
      .ifEqual { this.values normalizationComparedTo other.values }

  fun isComplete() = keys.isEmpty()

  companion object {
    private val empty: MapComponent = MapComponent(PartitionedTypeUnion.complete(), MaybeAbsent.empty())
    private val complete: MapComponent = MapComponent(PartitionedTypeUnion.empty(), MaybeAbsent.complete())
    internal fun empty() = empty
    internal fun complete() = complete
  }
}
