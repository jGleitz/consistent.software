package software.consistent.model.type

import software.consistent.model.value.Value

public sealed interface Type {
  public infix fun or(other: Type): Type = AnyOf(this, other)
  public infix fun and(other: Type): Type = AllOf(this, other)
  public infix fun butNot(other: Type): Type = ButNot(this, other)

  public operator fun contains(value: Value): Boolean
  public operator fun contains(type: Type): Boolean

  public typealias Any = software.consistent.model.type.Any
}
