package software.consistent.data.type.solvers

import software.consistent.data.type.solvers.generic.EnumeratedValues
import software.consistent.data.type.solvers.generic.EnumerationRepresented
import software.consistent.data.value.Value
import software.consistent.formatting.monospacelayout.Monospace

internal sealed interface SymbolSet : EnumerationRepresented<Value.Symbol, SymbolSet> {
  private class Positive(members: NormalizationOrdered<Value.Symbol>) :
      EnumeratedValues.Positive<Value.Symbol, SymbolSet>(members), SymbolSet {
    override val asRep: SymbolSet
      get() = this

    override val enumeration: EnumeratedValues<Value.Symbol, SymbolSet>
      get() = this

    override fun positive(members: NormalizationOrdered<Value.Symbol>): SymbolSet =
        SymbolSet.Positive(members)

    override fun negative(members: NormalizationOrdered<Value.Symbol>): SymbolSet =
        SymbolSet.Negative(members)
  }

  private class Negative(members: NormalizationOrdered<Value.Symbol>) :
      EnumeratedValues.Negative<Value.Symbol, SymbolSet>(members), SymbolSet {
    override val asRep: SymbolSet
      get() = this

    override val enumeration: EnumeratedValues<Value.Symbol, SymbolSet>
      get() = this

    override fun positive(members: NormalizationOrdered<Value.Symbol>): SymbolSet =
        SymbolSet.Positive(members)

    override fun negative(members: NormalizationOrdered<Value.Symbol>): SymbolSet =
        SymbolSet.Negative(members)

    override fun allMembers() = symbolMembers()

    override val completeLayout
      get() = Monospace.atom("AnySymbol")
  }

  companion object {
    private val empty = Positive(NormalizationOrdered.empty())
    private val complete = Negative(NormalizationOrdered.empty())

    fun empty(): SymbolSet = empty

    fun complete(): SymbolSet = complete

    fun of(vararg values: Value.Symbol): SymbolSet = Positive(NormalizationOrdered.of(*values))
  }
}

/** Every UTF-16 string, by length, with the alphabet rotated to start with a printable letter. */
private fun symbolMembers(): Sequence<Value.Symbol> = sequence {
  yield(Value.Symbol(""))
  var digits = intArrayOf(0)
  while (true) {
    yield(Value.Symbol(digits.map { ((it + 'a'.code) % 65536).toChar() }.joinToString("")))
    var index = digits.lastIndex
    while (index >= 0 && digits[index] == 65535) {
      digits[index] = 0
      index -= 1
    }
    if (index < 0) digits = IntArray(digits.size + 1) else digits[index] += 1
  }
}
