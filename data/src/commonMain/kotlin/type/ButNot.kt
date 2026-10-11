package software.consistent.data.type

import software.consistent.data.formatting.monospacelayout.Monospace
import software.consistent.data.formatting.monospacelayout.WithMonospaceLayout

public sealed interface ButNot : Type {
  public val base: Type
  public val exception: Type

  public companion object {
    public operator fun invoke(base: Type, exception: Type): Type = ButNotImpl(base, exception)
  }
}

private class ButNotImpl(override val base: Type, override val exception: Type) : ButNot, WithMonospaceLayout,
  Queryable by UsingSolver(base.solver without exception.solver) {
  override val monospaceLayout
    get() = Monospace.line(separator = "&!", 2) {
      -base.monospaceLayout
      -exception.monospaceLayout
    }

  override fun toString() = monospaceLayout.renderToString()
}
