package software.consistent.data.type

import software.consistent.data.type.solvers.*
import software.consistent.data.value.Value
import kotlin.jvm.JvmInline

@JvmInline
internal value class UsingSolver(
  override val solver: SolvableSet,
) : Queryable {
  constructor(symbolsSolver: ValueUnion<Value.Symbol>) : this(PartitionedTypeUnion.empty().copy(symbols = symbolsSolver))
  constructor(numbersSolver: DisjunctIntervalUnion<Value.Number>) : this(PartitionedTypeUnion.empty().copy(numbers = numbersSolver))
  constructor(mapSolver: MapSolver) : this(PartitionedTypeUnion.empty().copy(maps = mapSolver))

  override fun allows(value: Value) = value in solver

  override fun isSubtypeOf(type: Type) = this.solver isSubsetOf type.solver
}
