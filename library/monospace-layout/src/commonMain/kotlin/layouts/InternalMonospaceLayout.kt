package software.consistent.data.formatting.monospacelayout.layouts

import software.consistent.data.formatting.monospacelayout.*

internal sealed interface InternalMonospaceLayout :
  MonospaceLayout,
  Renderable {
  /**
   * Approximates how wide the layout would be if rendered in a single line. [PrintWidthIfSupported.NOT_SUPPORTED] indicates that this
   * layout should not be printed in a single line. Otherwise, the result contains the number of columns the layout will take up.
   */
  context(layout: LayoutContext)
  fun singleLinePrintWidth(): PrintWidthIfSupported

  /**
   * Renders this layout. Must not emit any newlines and must not exceed the width indicated by [singleLinePrintWidth] unless the indicated
   * width was [PrintWidthIfSupported.NOT_SUPPORTED].
   */
  context(layout: LayoutContext)
  fun renderInSingleLineTo(target: LayoutTarget)

  /**
   * Renders this layout. May emit newlines and is not bound by the width indicated by [singleLinePrintWidth].
   */
  context(layout: LayoutContext)
  fun renderMultilineTo(target: LayoutTarget)

  context(layout: LayoutContext)
  override fun renderTo(target: LayoutTarget) {
    val singleLineWidth = singleLinePrintWidth()
    if (singleLineWidth.isSupportedAnd { it <= layout.state.remainingWidthBeforeLineBreak }) {
      renderInSingleLineTo(target)
    } else {
      renderMultilineTo(target)
    }
  }

  override fun renderToString(settings: LayoutSettings): String {
    val target = LayoutTarget(StringBuilder())
    context(LayoutContext(settings)) {
      this.renderTo(target)
    }
    return target.buildString()
  }

  companion object {
    @Suppress("UNCHECKED_CAST") // cast is safe because MonoSpaceLayout is sealed and all instances implement InternalMonoSpaceLayout
    internal fun intern(externalLayouts: List<MonospaceLayout>) = externalLayouts as List<InternalMonospaceLayout>

    internal fun intern(externalLayout: MonospaceLayout) =
      when (externalLayout) {
        is InternalMonospaceLayout -> externalLayout
      }
  }
}
