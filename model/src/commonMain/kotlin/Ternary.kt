package software.consistent.model

import software.consistent.model.Ternary.*

public sealed interface Ternary {
  public operator fun not(): Ternary
  public infix fun and(other: Ternary): Ternary
  public infix fun or(other: Ternary): Ternary

  public object Yes : Ternary {
    public override fun not(): No = No
    public override infix fun and(other: Ternary): Ternary = other
    override fun or(other: Ternary): Yes = Yes
  }

  public object No : Ternary {
    override fun not(): Yes = Yes
    override fun and(other: Ternary): No = No
    override fun or(other: Ternary): Ternary = other
  }

  public object Unknown : Ternary {
    override fun not(): Unknown = Unknown
    override fun and(other: Ternary): Ternary = if (other === No) No else Unknown
    override fun or(other: Ternary): Ternary = if (other === Yes) Yes else Unknown
  }
}

public fun surely(result: Ternary): Boolean = result === Yes
public fun possibly(result: Ternary): Boolean = result !== No
internal fun known(result: Boolean) = if (result) Yes else No

public inline infix fun Ternary.and(predicate: () -> Ternary): Ternary = when (this) {
  Yes -> predicate()
  No -> No
  Unknown -> if (possibly(predicate())) Unknown else No
}

public inline infix fun Ternary.or(predicate: () -> Ternary): Ternary = when (this) {
  Yes -> Yes
  No -> predicate()
  Unknown -> if (surely(predicate())) Yes else Unknown
}


public inline fun <T> Iterable<T>.any(predicate: (T) -> Ternary): Ternary {
  if (this is Collection && isEmpty()) return No
  var result: Ternary = No
  for (element in this) {
    when (predicate(element)) {
      Yes -> return Yes
      Unknown -> result = Unknown
      No -> Unit
    }
  }
  return result
}
