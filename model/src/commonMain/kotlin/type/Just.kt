package software.consistent.model.type

import software.consistent.model.type.solvers.Solvers
import software.consistent.model.value.Value

public sealed interface Just : Type {
  public val value: Value

  public companion object {
    public operator fun invoke(value: Value): Just = JustImpl(value)
  }
}

private class JustImpl(override val value: Value) : Just,
  SolvableType by UsingSolver(Solvers.just(value)) {
  override fun toString() = "Just($value)"
}
