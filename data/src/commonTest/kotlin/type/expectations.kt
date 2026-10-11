package software.consistent.model.type

import ch.tutteli.atrium.creating.Expect
import ch.tutteli.atrium.logic._logic
import software.consistent.model.value.Value

fun <Typ : Type> Expect<Typ>.toContain(value: Value) =
  _logic.createAndAppend("to contain", value) { value in it }

fun <Typ : Type> Expect<Typ>.notToContain(value: Value) =
  _logic.createAndAppend("not to contain", value) { value !in it }

fun <Typ : Type> Expect<Typ>.toContain(otherType: Type) =
  _logic.createAndAppend("to contain", otherType) { otherType in it }

fun <Typ : Type> Expect<Typ>.notToContain(otherType: Type) =
  _logic.createAndAppend("not to contain", otherType) { otherType !in it }
