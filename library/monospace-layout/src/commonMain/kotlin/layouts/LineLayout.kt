package software.consistent.data.formatting.monospacelayout.layouts

import software.consistent.data.formatting.monospacelayout.LayoutContext
import software.consistent.data.formatting.monospacelayout.LayoutTarget
import software.consistent.data.formatting.monospacelayout.PrintWidthIfSupported
import software.consistent.data.formatting.monospacelayout.PrintWidthIfSupported.Companion.NOT_SUPPORTED
import software.consistent.data.formatting.monospacelayout.Separator

internal class LineLayout(
  private val items: List<InternalMonospaceLayout>,
  private val separator: Separator,
) : InternalMonospaceLayout {
  override val isContainer get() = true

  context(layout: LayoutContext)
  override fun singleLinePrintWidth(): PrintWidthIfSupported {
    // should we consider this layout an ‘entered box’? We currently say ‘no’…
    if (items.size > layout.settings.maxItemsPerLine) return NOT_SUPPORTED

    return hypotheticalWidth {
      for ((index, item) in items.withIndex()) {
        val itemWidth = item.singleLinePrintWidth().getOrElse { return NOT_SUPPORTED }

        layout.state.remainingWidthBeforeLineBreak -= itemWidth
        if (index != items.lastIndex) {
          layout.state.remainingWidthBeforeLineBreak -= separator.singleLine.printWidth
        }

        if (layout.state.remainingWidthBeforeLineBreak < 0) {
          return NOT_SUPPORTED
        }
      }
    }
  }

  context(layout: LayoutContext)
  override fun renderInSingleLineTo(target: LayoutTarget) {
    for ((index, item) in items.withIndex()) {
      item.renderInSingleLineTo(target)
      if (index != items.lastIndex) {
        target.append(separator.singleLine)
      }
    }
  }

  context(layout: LayoutContext)
  override fun renderMultilineTo(target: LayoutTarget) {
    if (items.isEmpty()) return

    var index = 0
    items[index].renderTo(target)

    layout.indented {
      while (index < items.lastIndex) {
        index += 1
        target.appendNewline().appendIndent().append(separator.multiLine)
        items[index].renderTo(target)
      }
    }
  }
}
