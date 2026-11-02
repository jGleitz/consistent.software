package software.consistent.data.type.solvers

import software.consistent.data.type.solvers.maps.MapSet
import software.consistent.data.value.Value

internal object Solvers {
  fun anything(): SolvableSet = TypePartitionUnion.complete()
  fun nothing(): SolvableSet = TypePartitionUnion.empty()

  fun just(value: Value) = when (value) {
    is Value.Number -> TypePartitionUnion.empty().copy(numbers = DisjunctIntervalUnion.just(value))
    is Value.Symbol -> TypePartitionUnion.empty().copy(symbols = EnumeratedValues.of(value))
    is Value.Map -> TypePartitionUnion.empty().copy(maps = MapSet.just(value))
  }
}
