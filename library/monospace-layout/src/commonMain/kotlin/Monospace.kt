package software.consistent.model.formatting.monospacelayout

import software.consistent.model.formatting.monospacelayout.layouts.*
import kotlin.jvm.JvmInline

public object Monospace {
  // region atom
  public fun atom(content: CharSequence): MonospaceLayout = MonospaceAtom(content)

  public inline fun atom(builderFun: StringBuilder.() -> Unit): MonospaceLayout =
    atom(StringBuilder().apply(builderFun))

  public inline fun atom(expectedSize: Int, builderFun: StringBuilder.() -> Unit): MonospaceLayout =
    atom(StringBuilder(expectedSize).apply(builderFun))
  // endregion

  // region indented
  public fun indented(layout: MonospaceLayout): MonospaceLayout = IndentedLayout(intern(layout))
  // endregion

  // region list
  public fun list(
    items: List<MonospaceLayout>,
    prefix: CharSequence? = null,
    postfix: CharSequence? = null,
    separator: Separator = defaultListSeparator(),
  ): MonospaceLayout = ListLayout(intern(items), prefix, postfix, separator)

  public fun list(
    items: List<MonospaceLayout>,
    prefix: CharSequence? = null,
    postfix: CharSequence? = null,
    separator: String = "",
  ): MonospaceLayout = list(items, prefix, postfix, defaultListSeparator(base = separator))

  public inline fun list(
    prefix: CharSequence? = null,
    postfix: CharSequence? = null,
    separator: Separator = defaultListSeparator(),
    size: Int,
    block: ListBuilder.() -> Unit
  ): MonospaceLayout {
    val items = ListBuilder(ArrayList(size)).apply(block).items
    return list(items, prefix, postfix, separator)
  }

  public inline fun list(
    prefix: CharSequence? = null,
    postfix: CharSequence? = null,
    separator: String = "",
    size: Int,
    block: ListBuilder.() -> Unit
  ): MonospaceLayout = list(prefix, postfix, defaultListSeparator(separator), size, block)

  public inline fun list(
    prefix: CharSequence? = null,
    postfix: CharSequence? = null,
    separator: Separator = defaultListSeparator(),
    itemsGenerator: () -> List<MonospaceLayout>
  ): MonospaceLayout = list(itemsGenerator(), prefix, postfix, separator)

  public inline fun list(
    prefix: CharSequence? = null,
    postfix: CharSequence? = null,
    separator: String = "",
    itemsGenerator: () -> List<MonospaceLayout>
  ): MonospaceLayout = list(prefix, postfix, defaultListSeparator(separator), itemsGenerator)

  public fun defaultListSeparator(base: CharSequence = ""): Separator = Separator(base, appendIfSingleLine = " ", appendIfMultiline = "")

  @JvmInline
  public value class ListBuilder(private val _items: MutableList<MonospaceLayout>) {
    public val items: List<MonospaceLayout> get() =
      // Normally, we should create a defensive copy of _items. However, we think it’s very unlikely that somebody will hold on to or
      // modify the list accidentally. Ourrently, this builder has virtually zero overhead, so we don’t want to add the overhead for
      // copying the list if it doesn’t have any practical value.
      _items

    public operator fun MonospaceLayout.unaryMinus() {
      _items += this
    }

    public operator fun CharSequence.unaryMinus() {
      _items += MonospaceAtom(this)
    }

    public operator fun List<MonospaceLayout>.unaryMinus() {
      _items += this
    }
  }
  // endregion

  @Suppress("UNCHECKED_CAST") // cast is safe because MonoSpaceLayout is sealed and all instances implement InternalMonoSpaceLayout
  private fun intern(externalLayouts: List<MonospaceLayout>) = externalLayouts as List<InternalMonospaceLayout>
  private fun intern(externalLayout: MonospaceLayout) = when (externalLayout) {
    is InternalMonospaceLayout -> externalLayout
  }
}
