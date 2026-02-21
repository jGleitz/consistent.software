package software.consistent.model

import kotlin.jvm.JvmInline

@JvmInline
value class MString(val value: String) : MValue {
    override fun toString(): String = '"' + value + '"'
}

object TString : MType {
    override fun toString(): String = "String"
}

@JvmInline
value class MNumber(val value: Number) : MValue {
    override fun toString() = value.toString()
}
object TNumber : MType {
    override fun toString(): String = "Number"
}

class MSymbol(val description: String) : MValue {
    override fun toString() = "#$description"
}
object TSymbol : MType {
    override fun toString(): String = "Symbol"
}