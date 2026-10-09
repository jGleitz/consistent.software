package software.consistent.model.type

import software.consistent.model.type.solvers.SolvableSet

internal sealed interface SolvableType : Type {
  val solver: SolvableSet
}

internal val Type.solver: SolvableSet
  get() = when (this) {
    is SolvableType -> solver
  }
