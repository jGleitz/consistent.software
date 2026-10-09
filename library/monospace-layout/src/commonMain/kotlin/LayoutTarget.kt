package software.consistent.model.formatting.monospacelayout

import software.consistent.model.formatting.monospacelayout.LayoutSettings.Companion.NEWLINE
import kotlin.jvm.JvmInline

@JvmInline
internal value class LayoutTarget(private val out: Appendable) {
  @IgnorableReturnValue
  context(layout: LayoutContext)
  internal fun append(value: Char) = apply {
    out.append(value)
    layout.state.position += 1
  }

  @IgnorableReturnValue
  context(layout: LayoutContext)
  internal fun append(value: CharSequence) = apply {
    // we assume that value doesn’t contain newlines because all fragment implementation check their inputs
    out.append(value)
    layout.state.position += value.length
  }

  @IgnorableReturnValue
  context(layout: LayoutContext)
  internal fun appendNewline() = apply {
    out.append(NEWLINE)
    layout.state.position = 0
    layout.state.boxesEntered = 0
  }

  @IgnorableReturnValue
  context(layout: LayoutContext)
  internal fun appendIndent() = append(layout.state.indent)

  @IgnorableReturnValue
  context(layout: LayoutContext)
  internal fun append(renderable: Renderable) = apply {
    renderable.renderTo(this)
  }

  internal fun buildString() = out.toString()
}
