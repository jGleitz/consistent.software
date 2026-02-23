package software.consistent.model

import kotlin.js.JsExport

@JsExport
public data class MString(
    public val value: String,
) : MValue {
    override fun toString(): String = '"' + value + '"'
}

@JsExport
public object TString : MType {
    override fun toString(): String = "String"
}

@JsExport
public data class MNumber(
    public val value: Number,
) : MValue {
    override fun toString(): String = value.toString()
}

@JsExport
public object TNumber : MType {
    override fun toString(): String = "Number"
}

@JsExport
public class MSymbol(
    public val description: String,
) : MValue {
    override fun toString(): String = "#$description"
}

@JsExport
public object TSymbol : MType {
    override fun toString(): String = "Symbol"
}
