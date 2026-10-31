package software.consistent.data.formatting.monospacelayout

@ConsistentCopyVisibility
public data class LayoutSettings private constructor(
  val maxItemsPerLine: Int,
  val maxBoxOpenPerLine: Int,
  internal val breakLineAfterColumn: PrintWidth,
  internal val indent: CharSequenceWithWidth,
) {
  init {
    require(maxItemsPerLine >= 1) { "${::maxItemsPerLine.name} must be positive! (was $maxItemsPerLine)" }
    require(maxBoxOpenPerLine >= 1) { "${::maxBoxOpenPerLine.name} must be positive! (was $maxBoxOpenPerLine)" }
    require(breakLineAfterColumn >= PrintWidth(0)) { "${::breakLineAfterColumn.name} must not be negative! (was $breakLineAfterColumn)" }
    require(!indent.contains(NEWLINE_PATTERN)) { "${::indent.name} must not contain newlines!" }
  }

  public constructor(
    maxItemsPerLine: Int = DEFAULT_MAX_ITEMS_PER_LINE,
    maxBoxOpenPerLine: Int = DEFAULT_MAX_BOX_OPEN_PER_LINE,
    breakLineAfterColumn: Int = DEFAULT_BREAK_LINE_AFTER_LENGTH,
    indent: CharSequence = DEFAULT_INDENT,
  ) : this(maxItemsPerLine, maxBoxOpenPerLine, PrintWidth(breakLineAfterColumn), CharSequenceWithCachedWidth(indent))

  public fun copy(
    maxItemsPerLine: Int = this.maxItemsPerLine,
    maxBoxOpenPerLine: Int = this.maxBoxOpenPerLine,
    breakLineAfterLength: Int = this.breakLineAfterColumn.asInt(),
    indent: CharSequence = this.indent,
  ): LayoutSettings = LayoutSettings(maxItemsPerLine, maxBoxOpenPerLine, breakLineAfterLength, CharSequenceWithCachedWidth(indent))

  public companion object {
    internal val NEWLINE_PATTERN = Regex("\r\n|\r|\n")
    internal const val NEWLINE = '\n'

    private const val DEFAULT_MAX_ITEMS_PER_LINE = 7
    private const val DEFAULT_MAX_BOX_OPEN_PER_LINE = 3
    private const val DEFAULT_BREAK_LINE_AFTER_LENGTH = 100
    private const val DEFAULT_INDENT = "\t"

    public fun default(): LayoutSettings = LayoutSettings()
  }
}
