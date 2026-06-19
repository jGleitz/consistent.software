package software.consistent.model

internal data class MString(
  val value: String,
) : MType {
  override fun includedBy(other: MType): Boolean = other == this || other == TString

  override fun toString(): String = '"' + value + '"'
}

internal object TString : MType {
  override fun includedBy(other: MType): Boolean = other == TString || other.includes(this)

  override fun toString(): String = "String"
}

internal data class MNumber(
  val value: Number,
) : MType {
  override fun includedBy(other: MType): Boolean = other == this || other == TNumber

  override fun toString(): String = value.toString()
}

internal object TNumber : MType {
  override fun includedBy(other: MType): Boolean = other == TNumber

  override fun toString(): String = "Number"
}

internal class MSymbol(
  val description: String,
) : MType {
  override fun toString(): String = "#$description"

  override fun includedBy(other: MType): Boolean = other == this || other == TSymbol
}

internal object TSymbol : MType {
  override fun includedBy(other: MType): Boolean = other == TSymbol

  override fun toString(): String = "Symbol"
}
