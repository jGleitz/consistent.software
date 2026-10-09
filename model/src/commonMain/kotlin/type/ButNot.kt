package software.consistent.model.type

public sealed interface ButNot : Type {
  public val base: Type
  public val exception: Type

  public companion object {
    public operator fun invoke(base: Type, exception: Type): Type = ButNotImpl(base, exception)
  }
}

private class ButNotImpl(override val base: Type, override val exception: Type) : ButNot,
  SolvableType by UsingSolver(base.solver without exception.solver)
