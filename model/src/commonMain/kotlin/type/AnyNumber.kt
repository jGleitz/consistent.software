package software.consistent.model.type

import software.consistent.model.type.solvers.DisjunctIntervalUnion

public sealed interface AnyNumber : Type {
  public companion object {
    public operator fun invoke(): AnyNumber = AnyNumberImpl
  }
}

private object AnyNumberImpl : AnyNumber,
  SolvableType by UsingSolver(DisjunctIntervalUnion.complete())
