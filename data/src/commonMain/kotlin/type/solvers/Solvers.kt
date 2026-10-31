package software.consistent.data.type.solvers

import software.consistent.data.value.Value

internal object Solvers {
  fun anything(): SolvableSet = PartitionedTypeUnion.complete()

  fun nothing(): SolvableSet = PartitionedTypeUnion.empty()

  fun just(value: Value) =
    when (value) {
      is Value.Number -> PartitionedTypeUnion.empty().copy(numbers = DisjunctIntervalUnion.just(value))
      is Value.Symbol -> PartitionedTypeUnion.empty().copy(symbols = ValueUnion.of(value))
      is Value.Map -> PartitionedTypeUnion.empty().copy(maps = MapSolver.just(value))
    }
}
