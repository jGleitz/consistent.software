package software.consistent.data.formatting.monospacelayout

import kotlin.jvm.JvmInline

/** A nonnegative print width, or [NOT_SUPPORTED], represented without nullable value-class boxing. */
@JvmInline
internal value class PrintWidthIfSupported private constructor(private val value: Int) {
  val isSupported: Boolean get() = value >= 0

  inline fun isSupportedAnd(predicate: (PrintWidth) -> Boolean): Boolean = isSupported && predicate(PrintWidth(value))

  inline fun getOrElse(fallback: () -> PrintWidth): PrintWidth = if (isSupported) PrintWidth(value) else fallback()

  inline fun map(transform: (PrintWidth) -> PrintWidth): PrintWidthIfSupported =
    if (isSupported) supported(transform(PrintWidth(value))) else NOT_SUPPORTED

  companion object {
    val NOT_SUPPORTED = PrintWidthIfSupported(-1)

    fun supported(printWidth: PrintWidth): PrintWidthIfSupported {
      require(printWidth.asInt() >= 0) { "Supported print widths must not be negative!" }
      return PrintWidthIfSupported(printWidth.asInt())
    }
  }
}
