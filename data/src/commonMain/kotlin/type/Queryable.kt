package software.consistent.data.type

import software.consistent.data.type.solvers.SolvableSet

internal sealed interface Queryable : TypeQueries {
  val solver: SolvableSet
}

internal val Type.solver: SolvableSet
  get() =
    when (this) {
      is Queryable -> solver
    }
