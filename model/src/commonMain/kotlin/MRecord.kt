package software.consistent.model

import kotlin.js.JsExport
import kotlin.js.JsName

@JsExport
public data class MRecord(
    val entries: Map<String, MType>,
) {
    @JsName("of")
    public constructor(vararg entries: Pair<String, MType>) : this(entries.toMap())
}
