package software.consistent.model.type.solvers

import software.consistent.model.value.Value

/**
 * Represents a _partition_ of the set of all values \(\mathbb{V}\). That is: each enum member `e` represents a set Vₑ such that
 */
internal enum class ValuePartition {
  Number,
  Symbol,
  Map
}

internal val Value.partition
  get() = when (this) {
    is Value.Map -> ValuePartition.Map
    is Value.Number -> ValuePartition.Number
    is Value.Symbol -> ValuePartition.Symbol
  }
