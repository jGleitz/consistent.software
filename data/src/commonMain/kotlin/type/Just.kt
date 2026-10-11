package software.consistent.data.type

import software.consistent.data.formatting.monospacelayout.WithMonospaceLayout
import software.consistent.data.type.solvers.Solvers
import software.consistent.data.value.Value
import software.consistent.data.value.monospaceLayout

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
