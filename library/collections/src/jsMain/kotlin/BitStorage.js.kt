package software.consistent.collections

import kotlin.math.max

internal actual class BitStorage private constructor(private var words: IntArray) {
  actual constructor(capacity: Int) : this(IntArray((capacity + WORD_BITS - 1) ushr WORD_SHIFT))

  fun copyOf(): BitStorage = BitStorage(words.copyOf())

  actual fun get(index: Int): Boolean {
    val wordIndex = index ushr WORD_SHIFT
    return wordIndex < words.size && words[wordIndex] and (1 shl index) != 0
  }

  actual fun set(index: Int) {
    val wordIndex = index ushr WORD_SHIFT
    if (wordIndex >= words.size) {
      // Like java.util.BitSet, at least double the size, so that adding ascending elements takes
      // amortised constant time
      words = words.copyOf(max(2 * words.size, wordIndex + 1))
    }
    words[wordIndex] = words[wordIndex] or (1 shl index)
  }

  actual fun clear(index: Int) {
    val wordIndex = index ushr WORD_SHIFT
    words[wordIndex] = words[wordIndex] and (1 shl index).inv()
  }

  actual fun clear() {
    words.fill(0)
  }

  actual fun nextSetBit(fromIndex: Int): Int {
    var wordIndex = fromIndex ushr WORD_SHIFT
    if (wordIndex >= words.size) return -1
    // `shl` only uses the lowest five bits of the shift distance, so this masks out the bits before
    // fromIndex within its word
    var word = words[wordIndex] and (-1 shl fromIndex)
    while (word == 0) {
      wordIndex++
      if (wordIndex == words.size) return -1
      word = words[wordIndex]
    }
    return (wordIndex shl WORD_SHIFT) + word.countTrailingZeroBits()
  }
}

internal actual fun BitStorage.copy(): BitStorage = copyOf()

private const val WORD_SHIFT = 5
private const val WORD_BITS = 1 shl WORD_SHIFT
