package software.consistent.model.type

import software.consistent.model.type.solvers.Solvers

public sealed interface AllOf : Type {
  public val types: List<Type>

  public companion object {
    public operator fun invoke(types: List<Type>): AllOf = AllOfImpl(types)

    public operator fun invoke(vararg types: Type): AllOf = AllOfImpl(types.toList())
  }
}

private class AllOfImpl(override val types: List<Type>) : AllOf,
  SolvableType by UsingSolver(types.fold(Solvers.anything()) { solver, type -> solver overlapWith type.solver }) {
  override fun and(other: Type): Type = AllOf(types + other)
}
