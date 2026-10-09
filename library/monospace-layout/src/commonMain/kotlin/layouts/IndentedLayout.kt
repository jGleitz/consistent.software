package software.consistent.model.formatting.monospacelayout.layouts

import software.consistent.model.formatting.monospacelayout.LayoutContext
import software.consistent.model.formatting.monospacelayout.LayoutTarget
import software.consistent.model.formatting.monospacelayout.hypothetical
import software.consistent.model.formatting.monospacelayout.indented

/**
 * Causes its content to be indented by one more level.
 */
// TODO re-think this whole thing
internal class IndentedLayout(private val content: InternalMonospaceLayout) : InternalMonospaceLayout {
  context(layout: LayoutContext)
  override fun canRenderInSingleLine() = hypothetical {
    layout.state.position += layout.settings.indent.length
    indented {
      content.canRenderInSingleLine()
    }
  }

  context(layout: LayoutContext)
  override fun renderInSingleLineTo(target: LayoutTarget) = indented {
    target.append(layout.settings.indent)
    content.renderInSingleLineTo(target)
  }

  context(layout: LayoutContext)
  override fun renderMultilineTo(target: LayoutTarget) = indented {
    target.append(layout.settings.indent)
    content.renderMultilineTo(target)
  }
}
