package software.consistent.model.formatting.monospacelayout.layouts

import software.consistent.model.formatting.monospacelayout.LayoutContext
import software.consistent.model.formatting.monospacelayout.LayoutSettings
import software.consistent.model.formatting.monospacelayout.LayoutTarget
import software.consistent.model.formatting.monospacelayout.Renderable

internal sealed interface InternalMonospaceLayout : MonospaceLayout, Renderable {
  context(layout: LayoutContext)
  fun canRenderInSingleLine(): Boolean

  context(layout: LayoutContext)
  fun renderInSingleLineTo(target: LayoutTarget)

  context(layout: LayoutContext)
   fun renderMultilineTo(target: LayoutTarget)

  context(layout: LayoutContext)
  override fun renderTo(target: LayoutTarget) {
    if (canRenderInSingleLine()) renderInSingleLineTo(target)
    else renderMultilineTo(target)
  }

  override fun renderToString(settings: LayoutSettings): String {
    val target = LayoutTarget(StringBuilder())
    context(LayoutContext(settings)) {
      this.renderTo(target)
    }
    return target.buildString()
  }
}
