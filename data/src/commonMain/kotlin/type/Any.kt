package software.consistent.model.type

import software.consistent.model.formatting.monospacelayout.Monospace
import software.consistent.model.formatting.monospacelayout.WithMonospaceLayout
import software.consistent.model.type.solvers.PartitionedTypeUnion

public sealed interface Any : Type {
  public companion object {
    public operator fun invoke(): Any = AnyImpl
  }
}

private object AnyImpl : Any, WithMonospaceLayout, Queryable by UsingSolver(PartitionedTypeUnion.complete()) {
  override val monospaceLayout get() = Monospace.atom("Any")

  override fun toString() = monospaceLayout.renderToString()
}
