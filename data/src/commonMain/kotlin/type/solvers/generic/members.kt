package software.consistent.data.type.solvers.generic

/** Takes turns so an infinite sequence cannot hide a later sequence. */
internal fun <T> interleaveMembers(sources: List<Sequence<T>>): Sequence<T> = sequence {
  val iterators = sources.map { it.iterator() }.toMutableList()
  while (iterators.isNotEmpty()) {
    val iterator = iterators.iterator()
    while (iterator.hasNext()) {
      val source = iterator.next()
      if (source.hasNext()) yield(source.next()) else iterator.remove()
    }
  }
}
