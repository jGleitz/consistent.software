package software.consistent.model.formatting.monospacelayout.layouts

import software.consistent.model.formatting.monospacelayout.*
import software.consistent.model.formatting.monospacelayout.LayoutSettings.Companion.NEWLINE_PATTERN

internal class ListLayout(
  private val items: List<InternalMonospaceLayout>,
  private val prefix: CharSequence?,
  private val postfix: CharSequence?,
  private val separator: Separator
) : InternalMonospaceLayout {
  init {
    require((prefix == null) == (postfix == null)) { "prefix and postfix can only both be null!" }
    require(prefix == null || !prefix.contains(NEWLINE_PATTERN)) { "prefix must not contain newlines!" }
    require(postfix == null || !postfix.contains(NEWLINE_PATTERN)) { "postfix must not contain newlines!" }
  }

  private val itemsAsSingleLine: MutableList<StringBuilder> = []

  context(layout: LayoutContext)
  override fun renderInSingleLineTo(target: LayoutTarget): Unit = enter {
    check(itemsAsSingleLine.size == items.size) {
      "canPrintInSingleLine must be called before printInSingleLineTo!"
    }

    // canPrintInSingleLine was called ⇒ itemsAsSingleLine contains the rendered items
    prefix?.let { target.append(it) }
    for ((index, builder) in itemsAsSingleLine.withIndex()) {
      target.append(builder)
      if (index != itemsAsSingleLine.lastIndex) {
        target.append(separator.singleLine)
      }
    }
    postfix?.let { target.append(it) }
  }

  context(layout: LayoutContext)
  override fun canRenderInSingleLine(): Boolean {
    if (items.size > layout.settings.maxItemsPerLine) return false

    enter {
      if (layout.state.boxesEntered > layout.settings.maxBoxOpenPerLine) return false

      hypothetical {
        prefix?.let { layout.state.position += it.length }
        val postfixLength = postfix?.length ?: 0

        for ((index, item) in items.withIndex()) {
          // We cannot assume that the item will fit into a single line just because we already have its rendered layout in itemsAsSingleLine
          // For example, we might now be further down the line than when we rendered last time.
          if (!item.canRenderInSingleLine()) {
            return false
          }
          // Once we know it fits, we can re-use the previous render result
          if (index <= itemsAsSingleLine.lastIndex) {
            layout.state.position += itemsAsSingleLine[index].length
          } else {
            val itemOut = StringBuilder()
            item.renderInSingleLineTo(LayoutTarget(itemOut))
            itemsAsSingleLine += itemOut
          }

          if (index != items.lastIndex) {
            layout.state.position += separator.singleLine.length
          }

          if (layout.state.position + postfixLength > layout.settings.breakLineAfterLength) return false
        }

      }
    }
    return true
  }

  context(layout: LayoutContext)
  override fun renderMultilineTo(target: LayoutTarget): Unit = enter {
    indentedIf(prefix != null) {
      prefix?.let { target.append(it).appendNewline().appendIndent() }

      for ((itemIndex, item) in items.withIndex()) {
        // last time we checked canPrintSingleLine on the item, we had a different indent. Hence, we need to re-check.
        // TODO we might be able to skip the re-check if we saved how much ‘safety buffer’ there was at the end of the line
        // TODO consider shifting the position by the separator so the item will take it into account when calculating whether it’ll fit
        if (item.canRenderInSingleLine()) {
          if (itemIndex <= itemsAsSingleLine.lastIndex) {
            target.append(itemsAsSingleLine[itemIndex])
          } else {
            // TODO is there a good reason to populate itemsAsSingleLine here?
            item.renderInSingleLineTo(target)
          }
        } else {
          item.renderMultilineTo(target)
        }

        if (itemIndex != items.lastIndex) {
          target.append(separator.multiLine).appendNewline().appendIndent()
        }
      }
    }

    postfix?.let { target.appendNewline().appendIndent().append(it) }
  }

  context(layout: LayoutContext)
  private inline fun <R> enter(block: () -> R): R {
    layout.state.boxesEntered += 1
    try {
      return block()
    } finally {
      layout.state.boxesEntered -= 1
    }
  }
}
