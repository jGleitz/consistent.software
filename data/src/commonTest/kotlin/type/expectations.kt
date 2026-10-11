package software.consistent.data.type

import ch.tutteli.atrium.creating.Expect
import ch.tutteli.atrium.logic._logic
import software.consistent.data.value.Value

fun <Typ : Type> Expect<Typ>.toAllow(value: Value) =
  _logic.createAndAppend("to allow", value) { it.allows(value) }

fun <Typ : Type> Expect<Typ>.toForbid(value: Value) =
  _logic.createAndAppend("to forbid", value) { it.forbids(value) }

fun <Typ : Type> Expect<Typ>.toBeSubtypeOf(otherType: Type) =
  _logic.createAndAppend("to be a subtype of", otherType) { it.isSubtypeOf(otherType) }

fun <Typ : Type> Expect<Typ>.notToBeSubtypeOf(otherType: Type) =
  _logic.createAndAppend("not to be a subtype of", otherType) { !it.isSubtypeOf(otherType) }
