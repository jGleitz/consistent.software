package software.consistent.model.type.solvers

internal interface SetOperations<TPartition : SetOperations<TPartition>> {
  fun isEmpty(): Boolean

  /**
   * The set of all values that are in this set **or** in the [other set][otherSet] (‘union’).
   *
   * *Implementation Note*: Implementations should ensure that the trivial cases are handled efficiently. These include:
   *
   *  - `this == `[otherSet]
   *  - `this.`[isEmpty()][isEmpty]` || `[otherSet][otherSet]`.`[isEmpty()][isEmpty]
   *  - `this.`[isComplete()][isComplete]` || [otherSet][otherSet]`.`[isComplete()][isComplete]
   */
  infix fun with(otherSet: TPartition): TPartition

  /**
   * The set of all values that are in this set **and** in the [other set][otherSet] (‘intersection’).
   *
   * *Implementation Note*: Implementations should ensure that the trivial cases are handled efficiently. These include:
   *
   *  - `this == `[otherSet]
   *  - `this.`[isEmpty()][isEmpty]` || `[otherSet][otherSet]`.`[isEmpty()][isEmpty]
   *  - `this.`[isComplete()][isComplete]` || [otherSet][otherSet]`.`[isComplete()][isComplete]
   */
  infix fun overlapWith(otherSet: TPartition): TPartition
  operator fun not(): TPartition
  infix fun without(otherSet: TPartition): TPartition
  operator fun contains(largerSet: TPartition): Boolean
}
