package software.consistent.data.type

import software.consistent.data.formatting.monospacelayout.Monospace
import software.consistent.data.formatting.monospacelayout.WithMonospaceLayout
import software.consistent.data.type.solvers.Solvers

public sealed interface AnyOf : Type {
  public val types: List<Type>

  public companion object {
    public operator fun invoke(types: List<Type>): AnyOf = AnyOfImpl(types)

    public operator fun invoke(vararg types: Type): AnyOf = AnyOfImpl(types.toList())
  }
}

private class AnyOfImpl(override val types: List<Type>) : AnyOf, WithMonospaceLayout,
  Queryable by UsingSolver(types.fold(Solvers.nothing()) { solver, type -> solver with type.solver }) {
  override fun or(other: Type): Type = AnyOf(types + other)

  override val monospaceLayout
    get() = Monospace.line(separator = "|") {
      types.map { it.monospaceLayout }
    }

  override fun toString() = monospaceLayout.renderToString()
}
