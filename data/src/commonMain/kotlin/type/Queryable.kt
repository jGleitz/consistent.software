package software.consistent.model.type

import software.consistent.model.type.solvers.SolvableSet

internal sealed interface Queryable : TypeQueries {
  val solver: SolvableSet
}

internal val Type.solver: SolvableSet
  get() = when (this) {
    is Queryable -> solver
  }
