package software.consistent.data.type.solvers

import kotlin.jvm.JvmInline

internal class InvariantContext {
  inline fun <reified S> rejectCreating() = RejectionBuilder(S::class.simpleName)
}

@JvmInline
internal value class RejectionBuilder(
  private val className: String?,
) {
  fun with(
    parameterName: String,
    parameter: Any?,
  ) = RejectionMessageBuilder(className, parameterName, parameter)
}

internal class RejectionMessageBuilder(
  private val className: String?,
  private val parameterName: String,
  private val parameter: Any?,
) {
  fun because(reason: String): Nothing =
    throw AssertionError("Cannot create a ${className ?: "???"} with $parameterName = ${parameter?.toString() ?: "null"}: $reason")
}

internal fun solverInvariant(check: InvariantContext.() -> Unit) {
  InvariantContext().check()
}
