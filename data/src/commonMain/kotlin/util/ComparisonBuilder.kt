package software.consistent.data.util

import kotlin.jvm.JvmInline
import kotlin.jvm.JvmName

// TODO avoid boxing

internal fun <V : Comparable<V>> compare(left: V) = ComparableComparisonBuilder(left)
internal fun <V> compare(left: V) = NotComparableComparisonBuilder(left)
@JvmName("compareEachComparable")
internal fun <V : Comparable<V>> compareEach(left: Collection<V>) = CollectionOfComparableComparisonBuilder(left)
internal fun compareEach(left: BooleanArray) = BooleanArrayComparisonBuilder(left)
internal fun <V> compareEach(left: Collection<V>) = CollectionOfNotComparableComparisonBuilder(left)

@JvmInline
internal value class ComparableComparisonBuilder<V : Comparable<V>>(val left: V) {
  fun to(right: V) = ComparisonResult(left.compareTo(right))
  inline fun <F : Comparable<F>> to(right: V, by: (V) -> F) = compare(by(left)) to by(right)
}

@JvmInline
internal value class NotComparableComparisonBuilder<V>(val left: V) {
  inline fun <F : Comparable<F>> to(right: V, by: (V) -> F) = compare(by(left)).to(by(right))
  inline fun to(right: V, using: (V, V) -> Int) = ComparisonResult(using(left, right))
  inline fun <F1 : Comparable<F1>, F2 : Comparable<F2>> to(right: V, by: (V) -> F1, crossinline thenBy: (V) -> F2) = to(right, by).ifEqual {
    compare(left).to(right, by = thenBy)
  }
}

@JvmInline
internal value class BooleanArrayComparisonBuilder(val left: BooleanArray) {
  fun toEach(right: BooleanArray) =
    compare(left).to(right, by = { it.size })
      .ifEqual {
        for (i in left.indices) {
          compare(left[i]).to(right[i]).ifUnequal { return@ifEqual it }
        }
        ComparisonResult.equal
      }
}

@JvmInline
internal value class CollectionOfComparableComparisonBuilder<V : Comparable<V>>(val left: Collection<V>) {
  fun toEach(right: Collection<V>) = CollectionOfNotComparableComparisonBuilder(left).toEach(right, using = Comparable<V>::compareTo)
}

@JvmInline
internal value class CollectionOfNotComparableComparisonBuilder<V>(val left: Collection<V>) {
  inline fun toEach(right: Collection<V>, using: (V, V) -> Int) =
    compare(left).to(right, by = { it.size })
      .ifEqual {
        val leftIt = left.iterator()
        val rightIt = right.iterator()
        while (leftIt.hasNext()) {
          compare(leftIt.next()).to(rightIt.next(), using)
            .ifUnequal { return@ifEqual it }
        }
        ComparisonResult.equal
      }

}

@JvmInline
internal value class ComparisonResult(val result: Int) {
  inline fun ifEqual(comparison: () -> ComparisonResult) = when (result) {
    0 -> comparison()
    else -> this
  }

  internal inline fun ifUnequal(next: (ComparisonResult) -> Unit) = when (result) {
    0 -> Unit
    else -> next(this)
  }

  fun isLower(): Boolean = result < 0
  fun isHigher(): Boolean = result > 0
  fun isEqual(): Boolean = result == 0

  val case
    get() = when {
      result < 0 -> ComparisonCase.isLower
      result == 0 -> ComparisonCase.isEqual
      else -> ComparisonCase.isHigher
    }

  companion object {
    val equal get() = ComparisonResult(0)
  }
}

internal enum class ComparisonCase {
  isLower,
  isEqual,
  isHigher;

  inline fun ifEqual(comparison: () -> ComparisonCase): ComparisonCase = when (this) {
    isEqual -> comparison()
    isLower, isHigher -> this
  }
}
