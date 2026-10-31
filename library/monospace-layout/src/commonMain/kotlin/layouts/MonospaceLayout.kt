package software.consistent.data.formatting.monospacelayout.layouts

import software.consistent.data.formatting.monospacelayout.LayoutSettings

public sealed interface MonospaceLayout {
  /**
   * Whether this layout is a container layout, rendering other layouts inside it.
   */
  public val isContainer: Boolean

  public fun renderToString(settings: LayoutSettings = LayoutSettings()): String

  public fun withPrefix(prefix: CharSequence): MonospaceLayout = wrappedIn(prefix, "")

  public fun withPostfix(postfix: CharSequence): MonospaceLayout = wrappedIn("", postfix)

  public fun wrappedIn(
    prefix: CharSequence,
    postfix: CharSequence,
  ): MonospaceLayout = LayoutWrapper(this, prefix, postfix)

  public fun containerWrappedIn(
    prefix: CharSequence,
    postfix: CharSequence,
  ): MonospaceLayout = if (isContainer) wrappedIn(prefix, postfix) else this
}
