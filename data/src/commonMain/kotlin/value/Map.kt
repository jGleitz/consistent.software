package software.consistent.data.value

import kotlin.jvm.JvmName
import software.consistent.formatting.monospacelayout.Monospace
import software.consistent.formatting.monospacelayout.WithMonospaceLayout
import software.consistent.formatting.monospacelayout.layouts.MonospaceLayout

public sealed interface Map : Value {
  public val size: Int
  public val keys: Set<Value>
  public val values: Collection<Value>
  public val entries: Set<Map.Entry<Value, Value>>

  public fun isEmpty(): Boolean

  public fun hasKey(key: Value): Boolean

  public operator fun get(key: Value): Value?

  public companion object {
    @JvmName("of") public operator fun invoke(): Map = Empty

    @JvmName("of")
    public operator fun invoke(data: kotlin.collections.Map<Value, Value>): Map =
        if (data.isEmpty()) Empty else KotlinBasedMap(buildMap(data.size) { putAll(data) })

    @JvmName("of")
    public operator fun invoke(vararg data: Pair<Value, Value>): Map =
        if (data.isEmpty()) Empty else KotlinBasedMap(buildMap(data.size) { putAll(data) })

    @JvmName("of")
    public operator fun invoke(data: Iterable<Pair<Value, Value>>): Map =
        if (data is Collection) Map(data) else Map(buildMap { putAll(data) })

    @JvmName("of")
    public operator fun invoke(data: Collection<Pair<Value, Value>>): Map =
        if (data.isEmpty()) Empty else KotlinBasedMap(buildMap(data.size) { putAll(data) })

    @JvmName("of")
    public operator fun invoke(data: Sequence<Pair<Value, Value>>): Map =
        Map(buildMap { putAll(data) })
  }
}

/**
 * @param data A map built with [buildMap]: its views and their iterators and entries reject
 *   changes, so the collections this map exposes cannot change it, even if they are cast to mutable
 *   types.
 */
private data class KotlinBasedMap(val data: kotlin.collections.Map<Value, Value>) :
    Map, WithMonospaceLayout {
  override val size
    get() = data.size

  override val keys
    get() = data.keys

  override val values
    get() = data.values

  override val entries
    get() = data.entries

  override fun isEmpty() = data.isEmpty()

  override fun hasKey(key: Value) = data.containsKey(key)

  override fun get(key: Value) = data[key]

  override val monospaceLayout
    get() =
        Monospace.list(prefix = "{", separator = ",", postfix = "}") {
          data.entries.map { (key, value) ->
            Monospace.line(separator = "=", size = 2) {
              -key.monospaceLayout
              -value.monospaceLayout
            }
          }
        }

  override fun toString() = monospaceLayout.renderToString()
}

private object Empty : Map, WithMonospaceLayout {
  override val size
    get() = 0

  override val keys
    get() = emptySet<Value>()

  override val values
    get() = emptySet<Value>()

  override val entries
    get() = emptySet<Map.Entry<Value, Value>>()

  override fun isEmpty() = true

  override fun hasKey(key: Value) = false

  override fun get(key: Value) = null

  override val monospaceLayout: MonospaceLayout
    get() = Monospace.atom("{}")

  override fun toString() = monospaceLayout.renderToString()
}
