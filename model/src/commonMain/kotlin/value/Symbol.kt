package software.consistent.model.value

import software.consistent.model.formatting.monospacelayout.Monospace
import software.consistent.model.formatting.monospacelayout.WithMonospaceLayout
import kotlin.jvm.JvmName

public sealed interface Symbol : Value {
  public infix fun compareIdentifiersTo(other: Symbol): Int

  public companion object {
    @JvmName("of")
    public operator fun invoke(identifier: String): Symbol = IdentifiedSymbol(identifier)
  }
}

internal val Symbol.identifier
  get() = when (this) {
    is IdentifiedSymbol -> identifier
  }

internal class IdentifiedSymbol(val identifier: String) : Symbol, WithMonospaceLayout {
  override fun compareIdentifiersTo(other: Symbol): Int = when (other) {
    is IdentifiedSymbol -> identifier.compareTo(other.identifier)
  }

  override val monospaceLayout get() = Monospace.atom(identifier)

  override fun toString() = monospaceLayout.renderToString()
}
