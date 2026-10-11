package software.consistent.model.type

import software.consistent.model.formatting.monospacelayout.WithMonospaceLayout
import software.consistent.model.type.solvers.Solvers
import software.consistent.model.value.Value
import software.consistent.model.value.monospaceLayout

public sealed interface Just : Type {
  public val value: Value

  public companion object {
    public operator fun invoke(value: Value): Just = JustImpl(value)
  }
}

private class JustImpl(override val value: Value) : Just, WithMonospaceLayout,
  Queryable by UsingSolver(Solvers.just(value)) {
  override val monospaceLayout get() = value.monospaceLayout
  override fun toString() = monospaceLayout.renderToString()
}
