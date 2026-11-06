package software.consistent.data.formatting.monospacelayout.layouts

import software.consistent.data.formatting.monospacelayout.*
import software.consistent.data.formatting.monospacelayout.LayoutSettings.Companion.NEWLINE_PATTERN
import software.consistent.data.formatting.monospacelayout.layouts.InternalMonospaceLayout.Companion.intern

internal class LayoutWrapper private constructor(
  val inner: InternalMonospaceLayout,
  val prefix: CharSequenceWithWidth,
  val postfix: CharSequenceWithWidth,
) : InternalMonospaceLayout {
  init {
    require(!prefix.contains(NEWLINE_PATTERN)) { "${::prefix.name} must not contain newlines!" }
    require(!postfix.contains(NEWLINE_PATTERN)) { "${::postfix.name} must not contain newlines!" }
  }

  internal constructor(layout: MonospaceLayout, prefix: CharSequence, postfix: CharSequence) : this(
    intern(layout),
    CharSequenceWithCachedWidth(prefix),
    CharSequenceWithCachedWidth(postfix),
  )

  // we don’t see ourselves as a proper container, just a decorator
  override val isContainer: Boolean get() = inner.isContainer

  context(layout: LayoutContext)
  override fun singleLinePrintWidth(): PrintWidthIfSupported =
    inner.singleLinePrintWidth().map {
      it + prefix.printWidth +
        postfix.printWidth
    }

  context(layout: LayoutContext)
  override fun renderInSingleLineTo(target: LayoutTarget) {
    target.append(prefix)
    inner.renderInSingleLineTo(target)
    target.append(postfix)
  }

  context(layout: LayoutContext)
  override fun renderMultilineTo(target: LayoutTarget) {
    target.append(prefix)
    inner.renderMultilineTo(target)
    target.append(postfix)
  }

  override fun withPrefix(prefix: CharSequence): MonospaceLayout =
    LayoutWrapper(inner, CharSequenceWithCachedWidth(prefix) + this.prefix, this.postfix)

  override fun withPostfix(postfix: CharSequence): MonospaceLayout = LayoutWrapper(inner, this.prefix, this.postfix + postfix)
}
