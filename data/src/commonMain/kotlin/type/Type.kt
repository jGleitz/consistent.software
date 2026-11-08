package software.consistent.data.type

import software.consistent.data.value.Value

public interface TypeQueries {
  public fun allows(value: Value): Boolean

  public fun forbids(value: Value): Boolean = !allows(value)

  public fun isSubtypeOf(type: Type): Boolean
}

public sealed interface Type : TypeQueries {
  public infix fun or(other: Type): Type = AnyOf(this, other)

  public infix fun and(other: Type): Type = AllOf(this, other)

  public infix fun butNot(other: Type): Type = ButNot(this, other)

  public typealias Any = software.consistent.data.type.Any
}
