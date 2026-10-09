package software.consistent.model.type.solvers

private class ChainedIterable<T>(val first: Iterable<T>, val second: Iterable<T>) : Iterable<T> {
  override fun iterator(): Iterator<T> = ChainedIterator(first.iterator(), second)

  private class ChainedIterator<T>(var delegate: Iterator<T>, var next: Iterable<T>?) : Iterator<T> {
    override fun hasNext(): Boolean {
      val nextCopy = next
      if (!delegate.hasNext() && nextCopy != null) {
        delegate = nextCopy.iterator()
        next = null
      }
      return delegate.hasNext()
    }

    override fun next(): T = delegate.next()
  }
}

internal fun <T> Iterable<T>.then(next: Iterable<T>): Iterable<T> = ChainedIterable(this, next)
internal fun <T> Iterable<T>.then(next: T): Iterable<T> = ChainedIterable(this, listOf(next))

