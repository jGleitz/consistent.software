package software.consistent.model.value

import software.consistent.model.formatting.monospacelayout.Monospace
import software.consistent.model.formatting.monospacelayout.WithMonospaceLayout
import kotlin.jvm.JvmName

public sealed interface Map : Value {
  public val keys: Set<Value>
  public val entries: Set<Map.Entry<Value, Value>>
  public fun hasKey(key: Value): Boolean
  public operator fun get(key: Value): Value?

  public companion object {
    @JvmName("of")
    public operator fun invoke(data: kotlin.collections.Map<Value, Value>): Map = KotlinBasedMap(data)

    @JvmName("of")
    public operator fun invoke(vararg data: Pair<Value, Value>): Map = Map(mapOf(*data))

    @JvmName("of")
    public operator fun invoke(data: List<Pair<Value, Value>>): Map = Map(data.toMap())
  }
}

private data class KotlinBasedMap(val data: kotlin.collections.Map<Value, Value>) : Map, WithMonospaceLayout {
  override val keys get() = data.keys
  override val entries get() = data.entries
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
