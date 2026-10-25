package software.consistent.data.type

import software.consistent.data.formatting.monospacelayout.Monospace
import software.consistent.data.formatting.monospacelayout.WithMonospaceLayout

public sealed interface ButNot : Type {
  public val base: Type
  public val exceptions: List<Type>

  public companion object {
    public operator fun invoke(base: Type, exception: Type): Type = ButNotImpl(base, listOf(exception))
  }
}

private class ButNotImpl(override val base: Type, override val exceptions: List<Type>) : ButNot, WithMonospaceLayout,
  Queryable by UsingSolver(exceptions.fold(base.solver) { remainder, exception -> remainder without exception.solver }) {
  override val monospaceLayout
    get() = Monospace.line(separator = "&!", exceptions.size + 1) {
      -base.monospaceLayout.containerWrappedIn("(", ")")
      for (exception in exceptions) {
        -exception.monospaceLayout.containerWrappedIn("(", ")")
      }
    }

  override fun butNot(other: Type) = ButNotImpl(base, exceptions + other)

  override fun toString() = monospaceLayout.renderToString()
}
