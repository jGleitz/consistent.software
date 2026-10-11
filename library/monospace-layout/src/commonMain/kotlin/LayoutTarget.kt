package software.consistent.data.formatting.monospacelayout

import software.consistent.data.formatting.monospacelayout.LayoutSettings.Companion.NEWLINE
import kotlin.jvm.JvmInline

@JvmInline
internal value class LayoutTarget(private val out: Appendable) {
  @IgnorableReturnValue
  context(layout: LayoutContext)
  internal fun append(value: CharSequence) = when (value) {
    is CharSequenceWithWidth -> append(value)
    else -> append(value.toString())
  }

  @IgnorableReturnValue
  context(layout: LayoutContext)
  internal fun append(value: String) = apply {
    // we assume that value doesn’t contain newlines because all fragment implementations check their inputs
    out.append(value)
    layout.state.remainingWidthBeforeLineBreak -= value.printWidth
  }

  @IgnorableReturnValue
  context(layout: LayoutContext)
  internal fun append(value: CharSequenceWithWidth) = apply {
    // we assume that value doesn’t contain newlines because all fragment implementations check their inputs
    out.append(value)
    layout.state.remainingWidthBeforeLineBreak -= value.printWidth
  }

  @IgnorableReturnValue
  context(layout: LayoutContext)
  internal fun appendNewline() = apply {
    out.append(NEWLINE)
    layout.state.resetForNewLine()
  }

  @IgnorableReturnValue
  context(layout: LayoutContext)
  internal fun appendIndent() = append(layout.state.currentIndent)

  @IgnorableReturnValue
  context(layout: LayoutContext)
  internal fun append(renderable: Renderable) = apply {
    renderable.renderTo(this)
  }

  internal fun buildString() = out.toString()
}
