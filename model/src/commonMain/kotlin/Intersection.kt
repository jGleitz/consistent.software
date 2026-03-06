package software.consistent.model

internal data class Intersection(
  val types: List<MType>,
) : MType {
  constructor(vararg types: MType) : this(types.toList())

  override fun normalize(): MType {
    // TODO simplify types
    val typeSet = types.flatMap(::flatten).distinct()
    return when (typeSet.size) {
      1 -> typeSet.first()
      else -> Intersection(typeSet)
    }
  }

  override fun includedBy(other: MType): Boolean = types.all { it.includedBy(other) }

  override fun toString() = types.joinToString(prefix = "(", separator = " & ", postfix = ")")

  companion object {
    private fun flatten(type: MType): Sequence<MType> =
      when (type) {
        is Intersection -> type.types.asSequence()
        else -> sequenceOf(type)
      }
  }
}
