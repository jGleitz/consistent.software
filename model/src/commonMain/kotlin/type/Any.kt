package software.consistent.model.type

import software.consistent.model.type.solvers.PartitionedTypeUnion

public sealed interface Any : Type {
  public companion object {
    public operator fun invoke(): Any = AnyImpl
  }
}

private object AnyImpl : Any, SolvableType by UsingSolver(PartitionedTypeUnion.complete())
