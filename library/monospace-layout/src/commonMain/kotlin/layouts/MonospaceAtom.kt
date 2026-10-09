package software.consistent.model.formatting.monospacelayout.layouts

import software.consistent.model.formatting.monospacelayout.LayoutContext
import software.consistent.model.formatting.monospacelayout.LayoutSettings.Companion.NEWLINE_PATTERN
import software.consistent.model.formatting.monospacelayout.LayoutTarget

/**
 * A [layout][MonospaceLayout] that renders its content as-is, without any formatting applied.
 */
internal class MonospaceAtom(
  /**
   * The content that the layout will render verbatim. Must not contain any newlines.
   */
  private val content: CharSequence
) : InternalMonospaceLayout {
  init {
    require(!content.contains(NEWLINE_PATTERN)) { "atoms must not contain newlines!" }
  }

  context(layout: LayoutContext)
  override fun canRenderInSingleLine() = true

  context(layout: LayoutContext)
  override fun renderInSingleLineTo(target: LayoutTarget) {
    target.append(content)
  }

  context(layout: LayoutContext)
  override fun renderMultilineTo(target: LayoutTarget) =
    throw UnsupportedOperationException("An atom cannot be printed multiline!")

  public companion object {
    public inline operator fun invoke(builderFun: StringBuilder.() -> Unit): MonospaceAtom =
      MonospaceAtom(StringBuilder().apply(builderFun))

    public inline operator fun invoke(expectedSize: Int, builderFun: StringBuilder.() -> Unit): MonospaceAtom =
      MonospaceAtom(StringBuilder(expectedSize).apply(builderFun))
  }
}
