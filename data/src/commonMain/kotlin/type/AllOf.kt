package software.consistent.model.type

import software.consistent.model.formatting.monospacelayout.Monospace
import software.consistent.model.formatting.monospacelayout.WithMonospaceLayout
import software.consistent.model.type.solvers.Solvers

public sealed interface AllOf : Type {
  public val types: List<Type>

  public companion object {
    public operator fun invoke(types: List<Type>): AllOf = AllOfImpl(types)

    public operator fun invoke(vararg types: Type): AllOf = AllOfImpl(types.toList())
  }
}

private class AllOfImpl(override val types: List<Type>) : AllOf, WithMonospaceLayout,
  Queryable by UsingSolver(types.fold(Solvers.anything()) { solver, type -> solver overlapWith type.solver }) {
  override fun and(other: Type): Type = AllOf(types + other)

  override val monospaceLayout
    get() = Monospace.line("&") {
      types.map { it.monospaceLayout }
    }

  override fun toString() = monospaceLayout.renderToString()
}
