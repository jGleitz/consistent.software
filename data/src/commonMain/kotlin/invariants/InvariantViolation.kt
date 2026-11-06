package software.consistent.data.invariants

internal class InvariantViolation(
  message: String,
) : Error(message) {
  companion object {
    inline operator fun invoke(messageBuilder: (StringBuilder) -> Unit) =
      InvariantViolation(StringBuilder().apply(messageBuilder).toString())
  }
}
