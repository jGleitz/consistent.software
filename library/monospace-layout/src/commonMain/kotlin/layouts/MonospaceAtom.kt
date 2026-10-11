package software.consistent.data.formatting.monospacelayout.layouts

import software.consistent.data.formatting.monospacelayout.CharSequenceWithCachedWidth
import software.consistent.data.formatting.monospacelayout.CharSequenceWithWidth
import software.consistent.data.formatting.monospacelayout.LayoutContext
import software.consistent.data.formatting.monospacelayout.LayoutSettings.Companion.NEWLINE_PATTERN
import software.consistent.data.formatting.monospacelayout.LayoutTarget
import software.consistent.data.formatting.monospacelayout.PrintWidthIfSupported.Companion.supported

/**
 * A [layout][MonospaceLayout] that renders its content as-is, without any formatting applied.
 */
internal class MonospaceAtom(
  /**
   * The content that the layout will render verbatim. Must not contain any newlines.
   */
  private val content: CharSequenceWithWidth
) : InternalMonospaceLayout {
  init {
    require(!content.contains(NEWLINE_PATTERN)) { "atoms must not contain newlines!" }
  }

  constructor(content: CharSequence) : this(CharSequenceWithCachedWidth(content))

  override val isContainer get() = false

  context(layout: LayoutContext)
  override fun singleLinePrintWidth() = supported(content.printWidth)

  context(layout: LayoutContext)
  override fun renderInSingleLineTo(target: LayoutTarget) {
    target.append(content)
  }

  context(layout: LayoutContext)
  override fun renderMultilineTo(target: LayoutTarget) = renderInSingleLineTo(target)

  context(layout: LayoutContext)
  override fun renderTo(target: LayoutTarget) = renderInSingleLineTo(target)

  companion object {
    inline operator fun invoke(builderFun: StringBuilder.() -> Unit): MonospaceAtom =
      MonospaceAtom(StringBuilder().apply(builderFun))

    inline operator fun invoke(expectedSize: Int, builderFun: StringBuilder.() -> Unit): MonospaceAtom =
      MonospaceAtom(StringBuilder(expectedSize).apply(builderFun))
  }
}
