package software.consistent.model.formatting.monospacelayout

context(layout: LayoutContext)
internal inline fun <R> indented(block: () -> R) = indentedIf(true, block)

context(layout: LayoutContext)
internal inline fun <R> indentedIf(condition: Boolean, block: () -> R): R {
  val lengthBefore = layout.state.indent.length
  if (condition) {
    layout.state.indent.append(layout.settings.indent)
  }
  try {
    return block()
  } finally {
    if (condition) {
      layout.state.indent.setLength(lengthBefore)
    }
  }
}

context(layout: LayoutContext)
internal inline fun <R> hypothetical(block: () -> R): R {
  val startPosition = layout.state.position
  try {
    return block()
  } finally {
    layout.state.position = startPosition
  }
}
