package software.consistent.model.type

import software.consistent.model.type.solvers.ValueUnion

public sealed interface AnySymbol : Type {
  public companion object {
    public operator fun invoke(): AnySymbol = AnySymbolImpl
  }
}

private object AnySymbolImpl : AnySymbol,
  SolvableType by UsingSolver(ValueUnion.complete())
