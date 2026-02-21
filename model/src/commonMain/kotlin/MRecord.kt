package software.consistent.model

data class MRecord(val entries: Map<String, MType>) {
    constructor(vararg entries: Pair<String, MType>) : this(entries.toMap())
}