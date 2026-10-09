package software.consistent.model.formatting.monospacelayout

public data class LayoutSettings(
  val maxItemsPerLine: Int = 7,
  val maxBoxOpenPerLine: Int = 3,
  val breakLineAfterLength: Int = 100,
  val indent: CharSequence = "\t"
) {
  init {
    require(maxItemsPerLine >= 1) { "maxInlineItems must be positive! (was $maxItemsPerLine)" }
    require(maxBoxOpenPerLine >= 1) { "maxBoxOpenPerLine must be positive! (was $maxBoxOpenPerLine)" }
    require(breakLineAfterLength >= 0) { "breakAtLineLength must not be negative! (was $breakLineAfterLength)" }
    require(!indent.contains(NEWLINE_PATTERN)) { "indent must not contain newlines!" }
  }

  internal companion object {
    internal val NEWLINE_PATTERN = Regex("\r\n|\r|\n")
    internal const val NEWLINE = '\n'
  }
}


