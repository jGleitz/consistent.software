package software.consistent.model.type.solvers

import software.consistent.model.util.ComparisonCase
import software.consistent.model.util.ComparisonResult
import software.consistent.model.util.compare
import software.consistent.model.value.Value
import software.consistent.model.value.identifier
import kotlin.jvm.JvmInline

internal fun interface NormalizationComparable<Family : NormalizationComparable<Family>> {
  infix fun normalizationComparedTo(other: Family): ComparisonCase
}

private fun ValuePartition.compareWithinPartition(left: Value, right: Value): Int = when (this) {
  ValuePartition.Number -> compare(left as Value.Number).to(right as Value.Number)
  ValuePartition.Symbol -> compare(left as Value.Symbol).to(right as Value.Symbol, by = Value.Symbol::identifier)
  ValuePartition.Map -> TODO()
}.result

internal fun normalizationOrder(left: Value, right: Value) = compareForNormalization(left).to(right).result

internal fun compareForNormalization(left: Value) = NormalizationComparisonBuilderForValues(left)

@JvmInline
internal value class NormalizationComparisonBuilderForValues(private val left: Value) {
  fun to(right: Value): ComparisonResult {
    val leftPartition = left.partition
    return compare(leftPartition).to(right.partition)
      .ifEqual { compare(left).to(right, using = left.partition::compareWithinPartition) }
  }
}

internal infix fun Value.isNormalizedBefore(other: Value) = compareForNormalization(this).to(other).isLower()

internal infix fun Value.isNormalizedAfter(other: Value) = compareForNormalization(this).to(other).isHigher()

// TODO since values is ordered, the advantage of using a HashSet may be slim. Test whether an ArrayList or Array is faster.
@JvmInline
internal value class NormalizationOrdered<TValue : Value> private constructor(private val values: LinkedHashSet<TValue>) :
  Set<TValue> by values {
  operator fun plus(other: NormalizationOrdered<TValue>): NormalizationOrdered<TValue> {
    if (isEmpty()) return other
    if (other.isEmpty()) return this

    val resultValues = LinkedHashSet<TValue>(size + other.size)
    val thisIterator = iterator()
    val otherIterator = other.iterator()
    var thisValue = thisIterator.next()
    var otherValue = otherIterator.next()
    while (true) {
      if (thisValue == otherValue) {
        resultValues += thisValue
        if (thisIterator.hasNext() && otherIterator.hasNext()) {
          thisValue = thisIterator.next()
          otherValue = otherIterator.next()
        } else break
      } else if (thisValue isNormalizedBefore otherValue) {
        resultValues += thisValue
        if (thisIterator.hasNext()) thisValue = thisIterator.next()
        else {
          resultValues += otherValue
          break
        }
      } else {
        resultValues += otherValue
        if (otherIterator.hasNext()) otherValue = otherIterator.next()
        else {
          resultValues += thisValue
          break
        }
      }
    }

    thisIterator.forEach(resultValues::add)
    otherIterator.forEach(resultValues::add)

    return when (resultValues.size) {
      values.size -> this
      other.values.size -> other
      else -> NormalizationOrdered(resultValues)
    }
  }

  operator fun minus(other: NormalizationOrdered<TValue>): NormalizationOrdered<TValue> {
    if (isEmpty() || other.isEmpty()) return this

    val resultSet = LinkedHashSet(values)
    resultSet.removeAll(other.values)
    return when {
      resultSet.isEmpty() -> empty()
      else -> NormalizationOrdered(resultSet)
    }
  }

  infix fun intersectedWith(other: NormalizationOrdered<TValue>): NormalizationOrdered<TValue> {
    if (isEmpty() || other.isEmpty()) return empty()

    val resultSet = LinkedHashSet(values)
    resultSet.retainAll(other.values)
    return when {
      resultSet.isEmpty() -> empty()
      else -> NormalizationOrdered(resultSet)
    }
  }

  internal companion object {
    internal fun <TValue : Value> of(vararg values: TValue) = NormalizationOrdered(
      values
        .sortedWith(::normalizationOrder)
        .toCollection(LinkedHashSet(values.size))
    )

    private val empty = NormalizationOrdered<Nothing>(LinkedHashSet())

    @Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
    internal inline fun <TValue : Value> empty() = empty as NormalizationOrdered<TValue>
  }
}
