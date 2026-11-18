package software.consistent.data.type

import software.consistent.data.type.solvers.NumberSet
import software.consistent.formatting.monospacelayout.Monospace
import software.consistent.formatting.monospacelayout.WithMonospaceLayout

public sealed interface AnyNumber : Type {
  public companion object {
    public operator fun invoke(): AnyNumber = AnyNumberImpl
  }
}

private object AnyNumberImpl :
    AnyNumber, WithMonospaceLayout, Queryable by UsingSolver(NumberSet.complete()) {
  override val monospaceLayout
    get() = Monospace.atom("Number")

  override fun toString() = monospaceLayout.renderToString()
}
