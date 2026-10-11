package software.consistent.model.type

import software.consistent.model.type.solvers.*
import software.consistent.model.value.Value
import kotlin.jvm.JvmInline

@JvmInline
internal value class UsingSolver(override val solver: SolvableSet) : Queryable {
  constructor(symbolsSolver: ValueUnion<Value.Symbol>) : this(PartitionedTypeUnion.empty().copy(symbols = symbolsSolver))
  constructor(numbersSolver: DisjunctIntervalUnion<Value.Number>) : this(PartitionedTypeUnion.empty().copy(numbers = numbersSolver))
  constructor(mapSolver: MapSolver) : this(PartitionedTypeUnion.empty().copy(maps = mapSolver))

  override fun contains(value: Value) = value in solver
  override fun contains(type: Type) = type.solver isSubsetOf this.solver
}
