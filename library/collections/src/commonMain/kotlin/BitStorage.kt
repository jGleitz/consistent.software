package software.consistent.collections

/**
 * The bits of a [BitSet]. Indices are never negative; callers check that. Bits beyond the allocated
 * words are unset, and [set] grows the storage as needed. The storage never shrinks.
 *
 * Each platform stores the bits in the word size that is efficient there:
 * - The JVM uses `java.util.BitSet`, which uses 64-bit words and the JIT’s intrinsics for bit
 *   scans.
 * - On JS, `Long` is emulated in software and a `LongArray` holds boxed objects. 32-bit words in an
 *   `IntArray`, which is an `Int32Array`, use the engine’s native integer operations instead.
 */
internal expect class BitStorage(capacity: Int) {
  fun get(index: Int): Boolean

  fun set(index: Int)

  /** Clears the bit at [index], which callers only call for set bits. */
  fun clear(index: Int)

  /** Clears all bits. */
  fun clear()

  /**
   * The index of the first set bit at or after [fromIndex], or `-1` if there is none. [fromIndex]
   * is never negative.
   */
  fun nextSetBit(fromIndex: Int): Int
}

/** An independent storage with the same capacity and bits. */
internal expect fun BitStorage.copy(): BitStorage
