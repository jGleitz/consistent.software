package software.consistent.data.formatting.monospacelayout

import software.consistent.data.formatting.monospacelayout.layouts.InternalMonospaceLayout.Companion.intern
import software.consistent.data.formatting.monospacelayout.layouts.LineLayout
import software.consistent.data.formatting.monospacelayout.layouts.ListLayout
import software.consistent.data.formatting.monospacelayout.layouts.MonospaceAtom
import software.consistent.data.formatting.monospacelayout.layouts.MonospaceLayout
import kotlin.jvm.JvmInline

public object Monospace {
  // region atom
  public fun atom(content: CharSequence): MonospaceLayout = MonospaceAtom(content)

  public inline fun atom(builderFun: AtomBuilder.() -> Unit): MonospaceLayout = atom(AtomBuilder().apply(builderFun).build())

  public inline fun atom(
    expectedSize: Int,
    builderFun: AtomBuilder.() -> Unit,
  ): MonospaceLayout = atom(AtomBuilder(expectedSize).apply(builderFun).build())

  @JvmInline
  public value class AtomBuilder(
    private val result: StringBuilder = StringBuilder(),
  ) : CharSequence by result {
    public constructor(expectedSize: Int) : this(StringBuilder(expectedSize))

    @PublishedApi
    internal fun build(): CharSequence = result

    @IgnorableReturnValue
    public fun append(content: CharSequence): AtomBuilder =
      apply {
        result.append(content)
      }

    @IgnorableReturnValue
    public fun append(char: Char): AtomBuilder =
      apply {
        result.append(char)
      }

    @IgnorableReturnValue
    public fun append(
      layout: MonospaceLayout,
      settings: LayoutSettings = LayoutSettings(),
    ): AtomBuilder =
      apply {
        context(LayoutContext(settings)) {
          intern(layout).renderTo(LayoutTarget(result))
        }
      }
  }
  // endregion

  // region list
  public fun list(
    items: List<MonospaceLayout>,
    prefix: CharSequence,
    postfix: CharSequence,
    separator: Separator = defaultListSeparator(),
  ): MonospaceLayout = ListLayout(intern(items), prefix, postfix, separator)

  public fun list(
    items: List<MonospaceLayout>,
    prefix: CharSequence,
    postfix: CharSequence,
    separator: String = "",
  ): MonospaceLayout = list(items, prefix, postfix, defaultListSeparator(base = separator))

  public inline fun list(
    prefix: CharSequence,
    postfix: CharSequence,
    separator: Separator = defaultListSeparator(),
    size: Int,
    block: ItemsBuilder.() -> Unit,
  ): MonospaceLayout {
    val items = ItemsBuilder(ArrayList(size)).apply(block).items
    return list(items, prefix, postfix, separator)
  }

  public inline fun list(
    prefix: CharSequence,
    postfix: CharSequence,
    separator: String = "",
    size: Int,
    block: ItemsBuilder.() -> Unit,
  ): MonospaceLayout = list(prefix, postfix, defaultListSeparator(separator), size, block)

  public inline fun list(
    prefix: CharSequence,
    postfix: CharSequence,
    separator: Separator = defaultListSeparator(),
    itemsGenerator: () -> List<MonospaceLayout>,
  ): MonospaceLayout = list(itemsGenerator(), prefix, postfix, separator)

  public inline fun list(
    prefix: CharSequence,
    postfix: CharSequence,
    separator: String = "",
    itemsGenerator: () -> List<MonospaceLayout>,
  ): MonospaceLayout = list(prefix, postfix, defaultListSeparator(separator), itemsGenerator)

  public fun defaultListSeparator(base: CharSequence = ""): Separator = Separator(singleLine = "$base ", multiLine = base)
  // endregion

  // region line
  public fun line(
    separator: Separator = defaultListSeparator(),
    items: List<MonospaceLayout>,
  ): MonospaceLayout = LineLayout(intern(items), separator)

  public fun line(
    separator: CharSequence,
    items: List<MonospaceLayout>,
  ): MonospaceLayout = LineLayout(intern(items), defaultLineSeparator(separator))

  public inline fun line(
    separator: Separator = defaultListSeparator(),
    itemGenerator: () -> List<MonospaceLayout>,
  ): MonospaceLayout = line(separator, itemGenerator())

  public inline fun line(
    separator: CharSequence,
    itemGenerator: () -> List<MonospaceLayout>,
  ): MonospaceLayout = line(separator, itemGenerator())

  public inline fun line(
    separator: Separator,
    size: Int,
    block: ItemsBuilder.() -> Unit,
  ): MonospaceLayout {
    val items = ItemsBuilder(ArrayList(size)).apply(block).items
    return line(separator, items)
  }

  public inline fun line(
    separator: CharSequence,
    size: Int,
    block: ItemsBuilder.() -> Unit,
  ): MonospaceLayout = line(defaultLineSeparator(separator), size, block)

  public fun defaultLineSeparator(base: CharSequence): Separator = Separator(" $base ", multiLine = "$base ")
  // endregion

  @JvmInline
  public value class ItemsBuilder(
    private val _items: MutableList<MonospaceLayout>,
  ) {
    public val items: List<MonospaceLayout>
      get() =
        // Normally, we should create a defensive copy of _items. However, we think it’s very unlikely that somebody will hold on to or
        // modify the list accidentally. Currently, this builder has virtually zero overhead, so we don’t want to add the overhead for
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
}
