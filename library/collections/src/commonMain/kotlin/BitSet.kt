package software.consistent.collections

/**
 * A mutable set of non-negative integers, stored as one bit for each integer from `0` up to the largest element.
 *
 * The set grows as needed, but never shrinks: its memory stays proportional to the largest element it ever contained, also after
 * [remove] or [clear]. It suits elements that are dense within `0..largest element`; for few elements spread over a large range,
 * a hash set needs less memory.
 *
 * [contains], [remove], and [size] take constant time, [add] takes amortised constant time.
 * [forEach] visits the elements in ascending order. It skips 32 or 64 absent elements at once, depending on the platform, so its
 * time is proportional to [size] plus a small fraction of the largest element.
 */
public class BitSet private constructor(private val bits: BitStorage, size: Int) {
  /** @param initialCapacity the number of integers, starting from `0`, that this set can hold before it first grows. */
  public constructor(initialCapacity: Int = 0) : this(BitStorage(requireValidInitialCapacity(initialCapacity)), size = 0)

  /** The number of elements in this set. */
  public var size: Int = size
    private set

  public fun isEmpty(): Boolean = size == 0

  public fun isNotEmpty(): Boolean = size != 0

  /** Whether [element] is in this set. Negative integers are never in this set. */
  public operator fun contains(element: Int): Boolean = element >= 0 && bits.get(element)

  /**
   * Adds [element] to this set, growing it if needed.
   *
   * @return `true` if [element] was not in this set before.
   * @throws IndexOutOfBoundsException if [element] is negative.
   */
  @IgnorableReturnValue
  public fun add(element: Int): Boolean {
    if (element < 0) throw IndexOutOfBoundsException("Cannot add $element to a BitSet: elements must not be negative.")
    if (bits.get(element)) return false
    bits.set(element)
    size++
    return true
  }

  /**
   * Removes [element] from this set.
   *
   * @return `true` if [element] was in this set before.
   */
  @IgnorableReturnValue
  public fun remove(element: Int): Boolean {
    if (element !in this) return false
    bits.clear(element)
    size--
    return true
  }

  /** Adds [element] to this set, like [add]. */
  public operator fun plusAssign(element: Int) {
    add(element)
  }

  /** Removes [element] from this set, like [remove]. */
  public operator fun minusAssign(element: Int) {
    remove(element)
  }

  /** Removes all elements from this set. */
  public fun clear() {
    bits.clear()
    size = 0
  }

  /**
   * Removes each element for which [predicate] returns `true`. Calls [predicate] once for each element, in ascending order.
   *
   * @return `true` if any element was removed.
   */
  @IgnorableReturnValue
  public inline fun removeAll(predicate: (element: Int) -> Boolean): Boolean {
    val sizeBefore = size
    // Removing the current element doesn’t disturb forEach: it continues the search after that element
    forEach { if (predicate(it)) remove(it) }
    return size != sizeBefore
  }

  /** A new set with the same elements as this one. Changes to either set don’t affect the other. */
  public fun copy(): BitSet = BitSet(bits.copy(), size)

  /**
   * The only element of this set.
   *
   * @throws NoSuchElementException if this set is empty.
   * @throws IllegalArgumentException if this set has more than one element.
   */
  public fun single(): Int =
    when (size) {
      0 -> throw NoSuchElementException("BitSet is empty.")
      1 -> nextElement(0)
      else -> throw IllegalArgumentException("BitSet has more than one element.")
    }

  /** Calls [action] for each element of this set, in ascending order. */
  // Potential optimisation: on JS, iterate word by word instead of calling nextElement for each element, as
  // TypedFastBitSet.js does: keep the word in a local, take its lowest set bit with countTrailingZeroBits, then clear it
  // with `word and (word - 1)`. This saves re-reading and masking the word per element. To keep this function inline
  // (no callback per element, non-local returns, which equals relies on), the inlined loop must reach the words, e.g.
  // through a @PublishedApi expect/actual inline helper. java.util.BitSet offers no word access, so the JVM would need its
  // own LongArray storage to do the same. Only worth it if a benchmark shows that forEach matters.
  public inline fun forEach(action: (element: Int) -> Unit) {
    var element = nextElement(0)
    while (element >= 0) {
      action(element)
      element = nextElement(element + 1)
    }
  }

  /** The smallest element that is at least [from], or `-1` if there is none. */
  @PublishedApi
  internal fun nextElement(from: Int): Int = bits.nextSetBit(from)

  /** Two bit sets are equal if they contain the same elements, regardless of how much memory they have allocated. */
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is BitSet || other.size != size) return false
    forEach { if (it !in other) return false }
    return true
  }

  /** Equals the hash code of a [Set] with the same elements. */
  override fun hashCode(): Int {
    var hashCode = 0
    forEach { hashCode += it }
    return hashCode
  }

  /** Lists the elements in ascending order, in the same format as a [Set]. */
  override fun toString(): String =
    buildString {
      append('[')
      var first = true
      // Without the label, forEach would iterate over the StringBuilder’s characters
      this@BitSet.forEach {
        if (!first) append(", ")
        append(it)
        first = false
      }
      append(']')
    }
}

private fun requireValidInitialCapacity(initialCapacity: Int): Int {
  require(initialCapacity >= 0) { "The initial capacity must not be negative, but was $initialCapacity." }
  return initialCapacity
}
