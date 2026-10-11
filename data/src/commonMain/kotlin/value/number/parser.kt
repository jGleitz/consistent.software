package software.consistent.data.value.number

import software.consistent.data.value.Value
import software.consistent.data.value.bignumbers.FRACTION_SLASH
import software.consistent.data.value.bignumbers.LOWERED_DIGITS
import software.consistent.data.value.bignumbers.ORDINARY_DIGITS
import software.consistent.data.value.bignumbers.RAISED_DIGITS
import software.consistent.exactnumbers.*
import software.consistent.exactnumbers.BigFractions.ZERO

internal fun parseNumberString(numberString: String) = NumberParser(numberString).parse()

private val BASE = 10.toBigFraction()
private val DIGIT_STYLES = listOf(ORDINARY_DIGITS, RAISED_DIGITS, LOWERED_DIGITS)

private class NumberParser(private val input: String) {
  private var position = 0
  private var result = ZERO

  fun parse(): Value.Number {
    skipWhitespace()
    val isNegative = readSign()
    if (endOfInput()) throw invalid("Expected at least one digit.")

    parseDigits()

    if (isNegative) result = -result
    return Value.Number(result)
  }

  private tailrec fun parseDigits() {
    if (isFractionSeparator(currentChar())) {
      throw invalid("Missing numerator before '${currentChar()}' at position ${position + 1}.")
    }

    val group = parseNumberGroup()
    skipWhitespace()

    if (!endOfInput() && isFractionSeparator(currentChar())) {
      group.verifyForRole(Numerator)
      parseFraction(group.value)
    } else {
      group.verifyForRole(Whole)
      result *= BASE toThePowerOf group.numberOfDigits
      result += group.value

      if (!endOfInput()) parseDigits()
    }
  }

  private fun parseNumberGroup(): NumberGroup {
    val start = position
    val groupStyle = DIGIT_STYLES.firstOrNull { currentChar() in it }
    var groupValue = ZERO
    var problemPosition: Int? = null

    while (!endOfInput() && !isSpace(currentChar()) && !isFractionSeparator(currentChar())) {
      val digit = groupStyle?.indexOf(currentChar()) ?: -1
      if (digit >= 0) groupValue = groupValue * BASE + digit.toBigFraction()
      else if (problemPosition == null) problemPosition = position
      nextChar()
    }

    val numberOfDigits = position - start
    return NumberGroup(groupValue, groupStyle, start, numberOfDigits, problemPosition)
  }

  private fun parseFraction(numerator: BigFraction) {
    val separatorPosition = position

    nextChar()
    skipWhitespace()

    if (endOfInput()) {
      throw invalid("Missing denominator after '${input[separatorPosition]}' at position ${separatorPosition + 1}.")
    }
    if (isFractionSeparator(currentChar())) throw anotherFraction()

    val denominator = parseNumberGroup()
    denominator.verifyForRole(Denominator)

    if (denominator.value == BigFractions.ZERO) {
      throw invalid("The denominator must not be zero (position ${denominator.start + 1}).")
    }
    result += numerator / denominator.value

    skipWhitespace()
    if (!endOfInput()) {
      throw if (isFractionSeparator(currentChar())) anotherFraction()
      else invalid(
        "The fraction must be at the end; unexpected '${currentChar().visible()}' at position ${position + 1} after the denominator."
      )
    }
  }

  private fun readSign(): Boolean = if (!endOfInput() && currentChar() == '-') {
    nextChar()
    skipWhitespace()
    true
  } else {
    false
  }

  private fun skipWhitespace() {
    while (!endOfInput() && isSpace(currentChar())) nextChar()
  }

  private fun endOfInput() = position >= input.length

  private fun currentChar() = input[position]

  private fun nextChar() {
    position += 1
  }

  private fun isSpace(char: Char) = char.category == SPACE_SEPARATOR

  private fun isFractionSeparator(char: Char) = char == '/' || char == FRACTION_SLASH

  private fun NumberGroup.verifyForRole(role: GroupRole) {
    if (style != null && style !in role.allowedStyles) throw invalidDigitAt(start, role)
    if (problemPosition != null) throw invalidDigitAt(problemPosition, role)
  }

  private fun invalidDigitAt(index: Int, role: GroupRole) = when (val char = input[index]) {
    in RAISED_DIGITS if role != Numerator ->
      invalid("Raised digit '$char' at position ${index + 1} is only allowed in a fraction's numerator.")

    in LOWERED_DIGITS if role != Denominator ->
      invalid("Lowered digit '$char' at position ${index + 1} is only allowed in a fraction's denominator.")

    '-' -> invalid("A minus sign is only allowed at the start of the number (position ${index + 1}).")
    '⁻', '₋' -> invalid("A minus sign is only allowed at the start of the number; use ordinary '-' (position ${index + 1}).")
    '+' -> invalid("A plus sign is not supported; omit '+' (position ${index + 1}).")
    '.', ',' ->
      invalid("Decimal notation is not supported; write a fraction such as '1/2' instead (position ${index + 1}).")

    else if role.allowedStyles.any { char in it } ->
      invalid("The ${role.description} must use ${role.expectedDigits} consistently; found '$char' at position ${index + 1}.")

    else -> invalid(
      "Unexpected character '${char.visible()}' at position ${index + 1} in the ${role.description}; expected ${role.expectedDigits}."
    )
  }

  private fun anotherFraction() =
    invalid("Only one fraction is allowed; found another '${currentChar()}' at position ${position + 1}.")

  private fun invalid(message: String) =
    IllegalArgumentException("Invalid number \"${input.escapedForErrorMessage()}\": $message")

  private fun Char.visible(): String = escapedForErrorMessage().replace("'", "\\'")

  private fun String.escapedForErrorMessage() = buildString {
    for (char in this@escapedForErrorMessage) append(char.escapedForErrorMessage())
  }

  private fun Char.escapedForErrorMessage(): String = when (this) {
    '\\' -> "\\\\"
    '"' -> "\\\""
    '\t' -> "\\t"
    '\r' -> "\\r"
    '\n' -> "\\n"
    else if (isISOControl() || category == FORMAT || category == LINE_SEPARATOR || category == PARAGRAPH_SEPARATOR) ->
      "\\u" + code.toString(16).padStart(4, '0')

    else -> toString()
  }
}

private data class NumberGroup(
  val value: BigFraction,
  val style: String?,
  val start: Int,
  val numberOfDigits: Int,
  val problemPosition: Int?
)

private enum class GroupRole(val description: String, val expectedDigits: String, val allowedStyles: Set<String>) {
  Whole("whole part", "ordinary digits (0–9)", setOf(ORDINARY_DIGITS)),
  Numerator("numerator", "ordinary or raised digits", setOf(ORDINARY_DIGITS, RAISED_DIGITS)),
  Denominator("denominator", "ordinary or lowered digits", setOf(ORDINARY_DIGITS, LOWERED_DIGITS)),
}
