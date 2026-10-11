package software.consistent.model.invariants

import software.consistent.model.util.appendIndented
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty0

/*
 * We use the inline modifier such that the KProperty and KFunction instances never leave the caller’s scope.
 * This should allow the JIT to inline those instances such that they never allocate heap space. But we haven’t
 * checked that theory yet.
 */

@Suppress("NOTHING_TO_INLINE") // using inline to never let KProperty0 leave the caller’s scope; see above
internal inline fun <V, Inv> KProperty0<V>.checkInvariant(invariant: Inv)
  where Inv : Function1<V, Boolean>, Inv : KFunction<Boolean> =
  checkInvariant(invariant.name, this, invariant)

@Suppress("NOTHING_TO_INLINE") // using inline to never let KProperty0 leave the caller’s scope; see above
internal inline fun <V> KProperty0<V>.checkInvariant(description: String, invariant: (V) -> Boolean) =
  checkInvariant(description, this, invariant)

@Suppress("NOTHING_TO_INLINE") // using inline to never let KProperty0 leave the caller’s scope; see above
internal inline fun <V> checkInvariant(invariantName: String, target: KProperty0<V>, invariant: (V) -> Boolean) {
  val targetValue = target()
  if (!invariant(targetValue)) {
    reportInvariantViolation(invariantName, target.name, targetValue)
  }
}

@Suppress("NOTHING_TO_INLINE") // using inline to never let KProperty0 leave the caller’s scope; see above
internal inline fun <L, R, Inv> KProperty0<L>.checkInvariant(with: KProperty0<R>, invariant: Inv)
  where Inv : Function2<L, R, Boolean>, Inv : KFunction<Boolean> =
  checkInvariant(invariant.name, this, with, invariant)

@Suppress("NOTHING_TO_INLINE") // using inline to never let KProperty0 leave the caller’s scope; see above
internal inline fun <L, R> checkInvariant(invariantName: String, left: KProperty0<L>, right: KProperty0<R>, invariant: (L, R) -> Boolean) {
  val leftValue = left()
  val rightValue = right()
  if (!invariant(leftValue, rightValue)) {
    reportInvariantViolation(invariantName, left.name, leftValue, right.name, rightValue)
  }
}

@Suppress("NOTHING_TO_INLINE") // using inline to never let KProperty0 leave the caller’s scope; see above
internal inline fun <V1, V2, V3, Inv> KProperty0<V1>.checkInvariant(with: KProperty0<V2>, andWith: KProperty0<V3>, invariant: Inv)
  where Inv : Function3<V1, V2, V3, Boolean>, Inv : KFunction<Boolean> =
  checkInvariant(invariant.name, this, with, andWith, invariant)

@Suppress("NOTHING_TO_INLINE") // using inline to never let KProperty0 leave the caller’s scope; see above
internal inline fun <V1, V2, V3> checkInvariant(
  invariantName: String,
  target1: KProperty0<V1>,
  target2: KProperty0<V2>,
  target3: KProperty0<V3>,
  invariant: (V1, V2, V3) -> Boolean
) {
  val value1 = target1()
  val value2 = target2()
  val value3 = target3()
  if (!invariant(value1, value2, value3)) {
    reportInvariantViolation(invariantName, target1.name, value1, target2.name, value2, target3.name, value3)
  }
}

/*
 * Generating the messages shouldn’t be inlined to every call site, so it’s delegated to normal functions.
 */

internal fun reportInvariantViolation(invariantName: String, violatorName: String, violatorValue: Any?) {
  throw InvariantViolation { message ->
    message.append(violatorName).append(" violates invariant ‘").append(invariantName).append("’:")
      .appendLine()
      .appendIndented(violatorValue)
  }
}

internal fun reportInvariantViolation(
  invariantName: String,
  firstViolatorName: String,
  firstViolatorValue: Any?,
  secondViolatorName: String,
  secondViolatorValue: Any?
) {
  throw InvariantViolation { message ->
    message.append(firstViolatorName).append(" and ").append(secondViolatorName)
      .append(" violate invariant ‘").append(invariantName).append("’:")
      .appendViolator(firstViolatorName, firstViolatorValue)
      .appendViolator(secondViolatorName, secondViolatorValue)
  }
}

internal fun reportInvariantViolation(
  invariantName: String,
  firstViolatorName: String,
  firstViolatorValue: Any?,
  secondViolatorName: String,
  secondViolatorValue: Any?,
  thirdViolatorName: String,
  thirdViolatorValue: Any?
) {
  throw InvariantViolation { message ->
    message.append(firstViolatorName).append(", ").append(secondViolatorName).append(", and ").append(thirdViolatorName)
      .append(" violate invariant ‘").append(invariantName).append("’:")
      .appendViolator(firstViolatorName, firstViolatorValue)
      .appendViolator(secondViolatorName, secondViolatorValue)
      .appendViolator(thirdViolatorName, thirdViolatorValue)
  }
}

@IgnorableReturnValue
private fun StringBuilder.appendViolator(name: String, value: Any?) =
  appendLine()
    .appendIndented(name).append(':')
    .appendLine()
    .appendIndented(asString(value), times = 2)

private fun asString(value: Any?) = when (value) {
  is BooleanArray -> value.joinToString(separator = ", ", prefix = "[", postfix = "]")
  else -> value.toString()
}
