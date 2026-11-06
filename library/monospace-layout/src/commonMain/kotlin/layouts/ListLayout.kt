package software.consistent.data.formatting.monospacelayout.layouts

import software.consistent.data.formatting.monospacelayout.*
import software.consistent.data.formatting.monospacelayout.LayoutSettings.Companion.NEWLINE_PATTERN
import software.consistent.data.formatting.monospacelayout.PrintWidthIfSupported.Companion.NOT_SUPPORTED

internal class ListLayout(
  private val items: List<InternalMonospaceLayout>,
  private val prefix: CharSequence,
  private val postfix: CharSequence,
  private val separator: Separator,
) : InternalMonospaceLayout {
  init {
    require(!prefix.contains(NEWLINE_PATTERN)) { "${::prefix.name} must not contain newlines!" }
    require(!postfix.contains(NEWLINE_PATTERN)) { "${::postfix.name} must not contain newlines!" }
  }

  override val isContainer get() = true

  context(layout: LayoutContext)
  override fun singleLinePrintWidth(): PrintWidthIfSupported {
    if (items.size > layout.settings.maxItemsPerLine ||
      layout.state.boxesEnteredInThisLine >= layout.settings.maxBoxOpenPerLine
    ) {
      return NOT_SUPPORTED
    }

    return hypotheticalWidth {
      layout.state.remainingWidthBeforeLineBreak -= prefix.printWidth
      val postfixPrintWidth = postfix.printWidth
      if (layout.state.remainingWidthBeforeLineBreak < postfixPrintWidth) return NOT_SUPPORTED

      enter {
        for ((index, item) in items.withIndex()) {
          val itemWidth = item.singleLinePrintWidth().getOrElse { return NOT_SUPPORTED }
          layout.state.remainingWidthBeforeLineBreak -= itemWidth

          if (index != items.lastIndex) {
            layout.state.remainingWidthBeforeLineBreak -= separator.singleLine.printWidth
          }
          if (layout.state.remainingWidthBeforeLineBreak < postfixPrintWidth) return NOT_SUPPORTED
        }
      }

      layout.state.remainingWidthBeforeLineBreak -= postfixPrintWidth
    }
  }

  context(layout: LayoutContext)
  override fun renderInSingleLineTo(target: LayoutTarget): Unit =
    enter {
      target.append(prefix)
      for ((index, item) in items.withIndex()) {
        item.renderInSingleLineTo(target)
        if (index != items.lastIndex) {
          target.append(separator.singleLine)
        }
      }
      target.append(postfix)
    }

  context(layout: LayoutContext)
  override fun renderMultilineTo(target: LayoutTarget): Unit =
    enter {
      target.append(prefix)
      layout.indented {
        target.appendNewline().appendIndent()

        for (index in 0..<items.lastIndex) {
          layout.state.reserveWidth(separator.multiLine.printWidth) {
            items[index].renderTo(target)
          }
          target.append(separator.multiLine).appendNewline().appendIndent()
        }
        items.last().renderTo(target)
      }

      target.appendNewline().appendIndent().append(postfix)
    }

  context(layout: LayoutContext)
  private inline fun <R> enter(block: () -> R): R {
    layout.state.boxesEnteredInThisLine += 1
    try {
      return block()
    } finally {
      layout.state.boxesEnteredInThisLine -= 1
    }
  }
}
