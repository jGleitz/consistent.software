package software.consistent.data.type.solvers

import software.consistent.data.formatting.monospacelayout.Monospace
import software.consistent.data.formatting.monospacelayout.WithMonospaceLayout
import software.consistent.data.type.solvers.ClosedSolvableSet.Cardinality.Companion.exactly
import software.consistent.data.value.Value

internal data class MapComponent(
  val keys: SolvableSet,
  val values: MaybeAbsent,
) : ValueSet.NormalizationComparable<Value.Map, MapComponent>,
  WithMonospaceLayout {
  init {
    require(values.includesAbsent || keys.isFinite() || values.values.isEmpty()) {
      // TODO better mechanism for checking
      "A required map component must have a finite set of keys"
    }
  }

  override fun contains(value: Value.Map): Boolean =
    if (values.includesAbsent) {
      value.entries.all { it.key !in keys || it.value in values.values }
    } else {
      var keyMatches = 0
      for ((key, value) in value.entries) {
        if (key in keys) {
          keyMatches += 1
          if (value !in values.values) return false
        }
      }
      // All keys present in value have the correct value. Now we have to check that all expected keys were present:
      // We rely our invariant that only finite keys may be required
      keys.cardinality(keyMatches) == exactly(keyMatches)
    }

  override fun normalizationComparedTo(other: MapComponent) =
    (this.keys normalizationComparedTo other.keys)
      .ifEqual { this.values normalizationComparedTo other.values }

  fun isComplete() = keys.isEmpty()

  override val monospaceLayout get() =
    Monospace.line("→", 2) {
      -keys.monospaceLayout.containerWrappedIn("(", ")")
      -values.monospaceLayout.containerWrappedIn("(", ")")
    }

  override fun toString() = monospaceLayout.renderToString()

  companion object {
    private val empty: MapComponent = MapComponent(PartitionedTypeUnion.complete(), MaybeAbsent.empty())
    private val complete: MapComponent = MapComponent(PartitionedTypeUnion.empty(), MaybeAbsent.complete())

    internal fun empty() = empty

    internal fun complete() = complete
  }
}
