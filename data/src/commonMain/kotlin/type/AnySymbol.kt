package software.consistent.data.type

import software.consistent.data.formatting.monospacelayout.Monospace
import software.consistent.data.formatting.monospacelayout.WithMonospaceLayout
import software.consistent.data.type.solvers.generic.EnumeratedValues

public sealed interface AnySymbol : Type {
  public companion object {
    public operator fun invoke(): AnySymbol = AnySymbolImpl
  }
}

private object AnySymbolImpl : AnySymbol, WithMonospaceLayout,
  Queryable by UsingSolver(EnumeratedValues.complete()) {
  override val monospaceLayout get() = Monospace.atom("Symbol")
  override fun toString() = monospaceLayout.renderToString()
}
