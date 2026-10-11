package software.consistent.model.generators

import io.kotest.property.Arb
import software.consistent.model.type.solvers.ValuePartition
import software.consistent.model.value.Value

internal val Arb.Companion.value get() = ValueArb

internal object ValueArb {
  val symbol = SymbolArb
  val number = NumberArb
  val map = MapArb

  // TODO self-referencing maps?
  internal operator fun invoke(structureSize: IntRange = 0..10, maxNesting: Int = 4) =
    Arb.value(ValuePartition.entries.toSet(), structureSize, maxNesting)

  internal operator fun invoke(partition: ValuePartition, structureSize: IntRange = 0..10, maxNesting: Int = 4): Arb<Value> {
    require(!structureSize.isEmpty()) { "structureSize must not be empty! (was $structureSize)" }
    require(structureSize.first >= 0) { "structureSize must not be negative! (was $structureSize)" }
    require(maxNesting >= 0) { "maxNesting must not be negative! (was $maxNesting)" }
    require(maxNesting >= 1 || partition.canContainOtherValues) {
      "Cannot create $partition values when nesting is not allowed! (maxNesting was $maxNesting)"
    }

    return when (partition) {
      ValuePartition.Number -> number()
      ValuePartition.Symbol -> symbol()
      ValuePartition.Map -> map(structureSize, maxNesting)
    }
  }

  internal operator fun invoke(vararg partitions: ValuePartition, structureSize: IntRange = 0..10, maxNesting: Int = 4) =
    Arb.value(partitions.toSet(), structureSize, maxNesting)

  internal operator fun invoke(partitions: Set<ValuePartition>, structureSize: IntRange = 0..10, maxNesting: Int = 4): Arb<Value> {
    require(!structureSize.isEmpty()) { "structureSize must not be empty! (was $structureSize)" }
    require(structureSize.first >= 0) { "structureSize must not be negative! (was $structureSize)" }
    require(maxNesting >= 0) { "maxNesting must not be negative! (was $maxNesting)" }
    require(partitions.isNotEmpty()) { "partitions must not be empty!" }

    return Choice(partitions.map { Arb.value(it, structureSize, maxNesting) })
  }
}

private val ValuePartition.canContainOtherValues: Boolean
  get() = when (this) {
    ValuePartition.Map -> true
    else -> false
  }


