package software.consistent.model.type

import software.consistent.model.formatting.monospacelayout.Monospace
import software.consistent.model.formatting.monospacelayout.WithMonospaceLayout
import software.consistent.model.type.solvers.DisjunctIntervalUnion

public sealed interface AnyNumber : Type {
  public companion object {
    public operator fun invoke(): AnyNumber = AnyNumberImpl
  }
}

private object AnyNumberImpl : AnyNumber, WithMonospaceLayout,
  Queryable by UsingSolver(DisjunctIntervalUnion.complete()) {
  override val monospaceLayout get() = Monospace.atom("Number")
  override fun toString() = monospaceLayout.renderToString()
}
