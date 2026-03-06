package software.consistent.model

internal data class MRecord(
  val entries: Map<String, MType>,
) : MType {
  constructor(vararg entries: Pair<String, MType>) : this(entries.toMap())

  override fun includedBy(other: MType): Boolean =
    other is MRecord &&
      entries.all { (key, type) ->
        other.entries[key]?.let { type.includedBy(it) } ?: true
      }
}
