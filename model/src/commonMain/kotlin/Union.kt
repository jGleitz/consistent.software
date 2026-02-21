package software.consistent.model

data class Union constructor(val types: List<MType>) : MType {
    constructor(vararg types: MType) : this(types.toList())

    override fun normalize(): MType {
        // TODO simplify types
        val typeSet = types.flatMap(::flatten).distinct()
        return when (typeSet.size) {
            1 -> typeSet.first()
            else -> Union(typeSet)
        }
    }

    override fun toString() = types.joinToString(prefix = "(", separator = " | ", postfix = ")")

    companion object {
        private fun flatten(type: MType): Sequence<MType> = when (type) {
            is Union -> type.types.asSequence().flatMap { flatten(it) }
            else -> sequenceOf(type)
        }
    }
}