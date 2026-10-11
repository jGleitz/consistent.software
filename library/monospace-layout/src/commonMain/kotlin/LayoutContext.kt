package software.consistent.data.formatting.monospacelayout

internal data class LayoutContext(val settings: LayoutSettings) {
  val state = LineState()

  internal inner class LineState {
    var remainingWidthBeforeLineBreak = settings.breakLineAfterColumn
    var boxesEnteredInThisLine: Int = 0
    val currentIndent: Indent = Indent()

    fun resetForNewLine() {
      remainingWidthBeforeLineBreak = settings.breakLineAfterColumn
      boxesEnteredInThisLine = 0
    }

    internal inline fun <R> reserveWidth(width: PrintWidth, block: () -> R): R {
      remainingWidthBeforeLineBreak -= width
      try {
        return block()
      } finally {
        remainingWidthBeforeLineBreak += width
      }
    }
  }

  internal inline fun <R> indented(block: () -> R): R {
    state.currentIndent.append(settings.indent)
    try {
      return block()
    } finally {
      state.currentIndent.remove(settings.indent)
    }
  }

  internal class Indent private constructor(private val value: StringBuilder) : CharSequenceWithWidth, CharSequence by value {
    internal constructor() : this(StringBuilder())

    var _printWidth: PrintWidth = PrintWidth(0)
    override val printWidth: PrintWidth get() = _printWidth

    override fun toString() = value.toString()

    fun append(value: CharSequenceWithWidth) {
      this.value.append(value)
      this._printWidth += value.printWidth
    }

    fun remove(value: CharSequenceWithWidth) {
      this.value.setLength(this.value.length - value.length)
      this._printWidth -= value.printWidth
    }
  }
}
