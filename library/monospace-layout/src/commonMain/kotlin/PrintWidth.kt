package software.consistent.data.formatting.monospacelayout

import io.github.kotlinmania.unicodewidth.unicodeWidth
import kotlin.jvm.JvmInline

/**
 * How many columns a string will take when printed in a monospace font. We use a distinct type to avoid accidentally using a string’s
 * length where the print width is required.
 */
@JvmInline
internal value class PrintWidth(
  private val value: Int,
) {
  operator fun plus(other: PrintWidth) = PrintWidth(this.value + other.value)

  operator fun minus(other: PrintWidth) = PrintWidth(this.value - other.value)

  operator fun compareTo(other: PrintWidth) = this.value - other.value

  operator fun compareTo(other: Int) = this.value - other

  fun asInt() = value
}

internal val CharSequence.printWidth: PrintWidth
  get() =
    when (this) {
      is CharSequenceWithWidth -> this.printWidth
      else -> PrintWidth(this.toString().unicodeWidth())
    }

internal interface CharSequenceWithWidth : CharSequence {
  val printWidth: PrintWidth

  operator fun plus(other: CharSequenceWithWidth): CharSequenceWithWidth =
    CharSequenceWithCachedWidth(this.toString() + other.toString(), this.printWidth + other.printWidth)

  operator fun plus(other: CharSequence) =
    CharSequenceWithCachedWidth(this.toString() + other.toString(), this.printWidth + other.printWidth)
}

internal class CharSequenceWithCachedWidth(
  private val string: CharSequence,
  override val printWidth: PrintWidth = string.printWidth,
) : CharSequenceWithWidth,
  CharSequence by string {
  override fun toString() = string.toString()
}
