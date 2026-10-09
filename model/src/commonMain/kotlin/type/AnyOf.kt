package software.consistent.model.type

import software.consistent.model.type.solvers.Solvers

public sealed interface AnyOf : Type {
  public val types: List<Type>

  public companion object {
    public operator fun invoke(types: List<Type>): AnyOf = AnyOfImpl(types)

    public operator fun invoke(vararg types: Type): AnyOf = AnyOfImpl(types.toList())
  }
}

private class AnyOfImpl(override val types: List<Type>) : AnyOf,
  SolvableType by UsingSolver(types.fold(Solvers.nothing()) { solver, type -> solver with type.solver }) {
  override fun or(other: Type): Type = AnyOf(types + other)
}
