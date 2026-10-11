package software.consistent.data.type

import software.consistent.data.formatting.monospacelayout.Monospace
import software.consistent.data.formatting.monospacelayout.WithMonospaceLayout
import software.consistent.data.formatting.monospacelayout.layouts.MonospaceLayout
import software.consistent.data.type.solvers.MapComponent
import software.consistent.data.type.solvers.MapSolver
import software.consistent.data.type.solvers.MaybeAbsent
import software.consistent.data.type.solvers.PartitionedTypeUnion

public object Map {
  public sealed interface Open : Type {
    public val components: List<Component>
    public val remainder: Type

    public companion object {
      public operator fun invoke(components: List<Component>, remainder: Type = Type.Any()): Map.Open = OpenMapImpl(components, remainder)
      public operator fun invoke(vararg components: Component, remainder: Type = Type.Any()): Map.Open =
        Map.Open(components.toList(), remainder)
    }
  }

  public sealed interface Closed : Type {
    public val components: List<Component>

    public companion object {
      public operator fun invoke(components: List<Component>): Map.Closed = ClosedMapImpl(components)
      public operator fun invoke(vararg components: Component): Map.Closed = ClosedMapImpl(components.toList())
    }
  }

  public sealed interface Component {
    public val key: Type
    public val keyRequired: Boolean
    public val value: Type

    public companion object {
      public fun required(types: Pair<Type, Type>): Map.Component = MapComponentImpl(types.first, keyRequired = true, types.second)
      public fun optional(types: Pair<Type, Type>): Map.Component = MapComponentImpl(types.first, keyRequired = false, types.second)
    }
  }
}

private class MapComponentImpl(override val key: Type, override val keyRequired: Boolean, override val value: Type) : Map.Component,
  WithMonospaceLayout {
  override val monospaceLayout: MonospaceLayout
    get() {
      return Monospace.line(separator = if (keyRequired) "=" else "?=", 2) {
        -key.monospaceLayout.containerWrappedIn("(", ")")
        -value.monospaceLayout
      }
    }

  override fun toString() = monospaceLayout.renderToString()
}

private val Map.Component.monospaceLayout
  get() = when (this) {
    is MapComponentImpl -> monospaceLayout
  }

private val Map.Component.solver
  get() = when (this) {
    is MapComponentImpl -> MapComponent(key.solver, MaybeAbsent(value.solver, includesAbsent = !keyRequired))
  }

private class ClosedMapImpl(override val components: List<Map.Component>) : Map.Closed, WithMonospaceLayout,
  Queryable by UsingSolver(closedSolver(components)) {
  override val monospaceLayout
    get() = Monospace.list(prefix = "{", separator = ",", postfix = "}", size = components.size + 1) {
      components.forEach {
        -it.monospaceLayout
      }
      -Monospace.atom("!...")
    }

  override fun toString() = monospaceLayout.renderToString()
}

private fun closedSolver(components: List<Map.Component>): MapSolver {
  var solver = MapSolver.complete()
  var remainingKeys = PartitionedTypeUnion.complete()
  for (component in components) {
    val keySolver = component.key.solver
    val componentSolver =
      MapSolver.just(MapComponent(keySolver, MaybeAbsent(component.value.solver, includesAbsent = !component.keyRequired)))
    solver = solver overlapWith componentSolver
    remainingKeys = remainingKeys without keySolver
  }
  if (!remainingKeys.isEmpty()) {
    solver = solver overlapWith MapSolver.just(MapComponent(remainingKeys, MaybeAbsent.onlyAbsent()))
  }
  return solver
}

private class OpenMapImpl(override val components: List<Map.Component>, override val remainder: Type) : Map.Open, WithMonospaceLayout,
  Queryable by UsingSolver(openSolver(components, remainder)) {
  override val monospaceLayout
    get() = Monospace.list(prefix = "{", separator = ",", postfix = "}", size = components.size + 1) {
      components.forEach {
        -it.monospaceLayout
      }
      -Monospace.atom("...")
    }

  override fun toString() = monospaceLayout.renderToString()
}

private fun openSolver(components: List<Map.Component>, remainder: Type): MapSolver {
  val remainderSolver = remainder.solver
  if (remainderSolver.isComplete()) {
    return MapSolver.allOf(components.map { it.solver })
  }

  var solver = MapSolver.complete()
  var remainingKeys = PartitionedTypeUnion.complete()
  for (component in components) {
    val keySolver = component.key.solver
    val componentSolver =
      MapSolver.just(MapComponent(keySolver, MaybeAbsent(component.value.solver, includesAbsent = !component.keyRequired)))
    solver = solver overlapWith componentSolver
    remainingKeys = remainingKeys without keySolver
  }
  if (!remainingKeys.isEmpty()) {
    solver = solver overlapWith MapSolver.just(MapComponent(remainingKeys, MaybeAbsent(remainderSolver, includesAbsent = true)))
  }
  return solver
}
