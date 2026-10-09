package software.consistent.model.formatting.monospacelayout

import software.consistent.model.formatting.monospacelayout.LayoutSettings.Companion.NEWLINE_PATTERN
import kotlin.jvm.JvmInline

// TODO consider whether this is necessary
public data class Separator(
  private val base: CharSequence = "",
  private val appendIfSingleLine: CharSequence = "",
  private val appendIfMultiline: CharSequence = ""
) {
  init {
    require(!base.contains(NEWLINE_PATTERN)) { "base must not contain newlines!" }
    require(!appendIfSingleLine.contains(NEWLINE_PATTERN)) { "appendIfSingleLine must not contain newlines!" }
    require(!appendIfMultiline.contains(NEWLINE_PATTERN)) { "appendIfMultiline must not contain newlines!" }
  }

  internal val singleLine get() = SingleLine(this)
  internal val multiLine get() = MultiLine(this)

  @JvmInline
  internal value class SingleLine(private val separator: Separator) : Renderable {
    context(layout: LayoutContext)
    override fun renderTo(target: LayoutTarget) {
      target.append(separator.base).append(separator.appendIfSingleLine)
    }

    internal val length get() = separator.base.length + separator.appendIfSingleLine.length
  }

  @JvmInline
  internal value class MultiLine(private val separator: Separator) : Renderable {
    context(layout: LayoutContext)
    override fun renderTo(target: LayoutTarget) {
      target.append(separator.base).append(separator.appendIfMultiline)
    }
  }
}
