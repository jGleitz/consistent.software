package software.consistent.model.type

import software.consistent.model.type.solvers.MapComponent
import software.consistent.model.type.solvers.MapSolver
import software.consistent.model.type.solvers.MaybeAbsent
import software.consistent.model.type.solvers.PartitionedTypeUnion

public object Map {
  public sealed interface Open : Type {
    public val components: List<Component>
    public val remainder: Type

    public companion object {
      public operator fun invoke(components: List<Component>, remainder: Type = Type.Any()): Map.Open = OpenMapImpl(components, remainder)
      public operator fun invoke(vararg components: Component, remainder: Type = Type.Any()): Map.Open = Map.Open(components.toList(), remainder)
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
      public fun required(key: Type, value: Type): Map.Component = MapComponentImpl(key, keyRequired = true, value)
      public fun optional(key: Type, value: Type): Map.Component = MapComponentImpl(key, keyRequired = false, value)
    }
  }
}

private class MapComponentImpl(override val key: Type, override val keyRequired: Boolean, override val value: Type) : Map.Component

private class ClosedMapImpl(override val components: List<Map.Component>) : Map.Closed,
  SolvableType by UsingSolver(closedSolver(components))

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

private class OpenMapImpl(override val components: List<Map.Component>, override val remainder: Type) : Map.Open,
  SolvableType by UsingSolver(openSolver(components, remainder))

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

private val Map.Component.solver
  get() = when (this) {
    is MapComponentImpl -> MapComponent(key.solver, MaybeAbsent(value.solver, includesAbsent = !keyRequired))
  }
