package software.consistent.collections

/**
 * Lazily yields one element from each input sequence in turn, in [sources] order. Exhausted inputs
 * are skipped until all inputs are exhausted. Infinite inputs do not starve later inputs.
 * Duplicates and nullable elements are preserved.
 *
 * Traversal starts only when the result is consumed. Each traversal obtains fresh input iterators;
 * whether the result can be traversed repeatedly depends on the input sequences.
 */
public fun <T> interleave(sources: List<Sequence<T>>): Sequence<T> = sequence {
  val iterators = sources.map { it.iterator() }.toMutableList()
  while (iterators.isNotEmpty()) {
    val iterator = iterators.iterator()
    while (iterator.hasNext()) {
      val source = iterator.next()
      if (source.hasNext()) yield(source.next()) else iterator.remove()
    }
  }
}
