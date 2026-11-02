package software.consistent.data.type

import software.consistent.data.type.solvers.*
import software.consistent.data.type.solvers.maps.MapSet
import software.consistent.data.value.Value
import kotlin.jvm.JvmInline

@JvmInline
internal value class UsingSolver(override val solver: SolvableSet) : Queryable {
  constructor(symbolsSolver: EnumeratedValues<Value.Symbol>) : this(TypePartitionUnion.empty().copy(symbols = symbolsSolver))
  constructor(numbersSolver: DisjunctIntervalUnion<Value.Number>) : this(TypePartitionUnion.empty().copy(numbers = numbersSolver))
  constructor(mapSolver: MapSet) : this(TypePartitionUnion.empty().copy(maps = mapSolver))

  override fun allows(value: Value) = value in solver
  override fun isSubtypeOf(type: Type) = this.solver isSubsetOf type.solver
}
