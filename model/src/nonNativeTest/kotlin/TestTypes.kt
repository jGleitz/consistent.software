import software.consistent.model.MNumber
import software.consistent.model.MString
import software.consistent.model.MSymbol
import software.consistent.model.MType
import software.consistent.model.TNumber
import software.consistent.model.TString
import software.consistent.model.TSymbol

object TestTypes {
  val strings = sequenceOf(MString("foo"), TString)
  val numbers = sequenceOf(MNumber(42), TNumber)
  val symbols = sequenceOf(MSymbol("test"), TSymbol)

  val all: Sequence<MType> =
    strings + numbers + symbols +
      sequenceOf(
        MString("foo") union MString("bar"),
        (MString("foo") union MString("bar")) intersect (MString("bar") union MString("baz")),
      )

  val withSupertypes: Sequence<WithSupertype> =
    sequenceOf(
      MString("foo") withSupertype TString,
      MNumber(42) withSupertype TNumber,
      MSymbol("test") withSupertype TSymbol,
    )

  data class WithSupertype(
    val type: MType,
    val supertype: MType,
  )

  private infix fun MType.withSupertype(supertype: MType) = WithSupertype(this, supertype)
}
