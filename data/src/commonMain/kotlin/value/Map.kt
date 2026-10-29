package software.consistent.data.value

import software.consistent.data.formatting.monospacelayout.Monospace
import software.consistent.data.formatting.monospacelayout.WithMonospaceLayout
import kotlin.jvm.JvmName

public sealed interface Map : Value {
  public val size: Int
  public val keys: Set<Value>
  public val values: Collection<Value>
  public val entries: Set<Map.Entry<Value, Value>>
  public fun isEmpty(): Boolean
  public fun hasKey(key: Value): Boolean
  public operator fun get(key: Value): Value?

  public companion object {
    @JvmName("of")
    public operator fun invoke(data: kotlin.collections.Map<Value, Value>): Map = KotlinBasedMap(buildMap { putAll(data) })

    @JvmName("of")
    public operator fun invoke(vararg data: Pair<Value, Value>): Map = KotlinBasedMap(buildMap { putAll(data) })

    @JvmName("of")
    public operator fun invoke(data: List<Pair<Value, Value>>): Map = KotlinBasedMap(buildMap { putAll(data) })
  }
}

/**
 * @param data A map built with [buildMap]: its views and their iterators and entries reject changes, so the collections this map
 * exposes cannot change it, even if they are cast to mutable types.
 */
private data class KotlinBasedMap(val data: kotlin.collections.Map<Value, Value>) : Map, WithMonospaceLayout {
  override val size get() = data.size
  override val keys get() = data.keys
  override val values get() = data.values
  override val entries get() = data.entries
  override fun isEmpty() = data.isEmpty()
  override fun hasKey(key: Value) = data.containsKey(key)
  override fun get(key: Value) = data[key]

  override val monospaceLayout
    get() = Monospace.list(prefix = "{", separator = ",", postfix = "}") {
      data.entries.map { (key, value) ->
        Monospace.line(separator = "=", size = 2) {
          -key.monospaceLayout
          -value.monospaceLayout
        }
      }
    }

  override fun toString() = monospaceLayout.renderToString()
}
