package software.consistent.data.type

import software.consistent.data.type.solvers.SymbolSet
import software.consistent.formatting.monospacelayout.Monospace
import software.consistent.formatting.monospacelayout.WithMonospaceLayout

public sealed interface AnySymbol : Type {
  public companion object {
    public operator fun invoke(): AnySymbol = AnySymbolImpl
  }
}

private object AnySymbolImpl :
    AnySymbol, WithMonospaceLayout, Queryable by UsingSolver(SymbolSet.complete()) {
  override val monospaceLayout
    get() = Monospace.atom("Symbol")

  override fun toString() = monospaceLayout.renderToString()
}
