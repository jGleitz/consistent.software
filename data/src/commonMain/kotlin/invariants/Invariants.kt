package software.consistent.data.invariants

import kotlin.jvm.JvmName
import kotlin.reflect.KProperty0

internal object Invariants {
  internal inline val <Comp : Comparable<Comp>> KProperty0<List<Comp>>.isNonDecreasing get() = checkInvariant(::isNonDecreasing)

  internal fun <Comp : Comparable<Comp>> isNonDecreasing(list: List<Comp>): Boolean =
    when (list.size) {
      0, 1 -> {
        true
      }

      else -> {
        val iterator = list.iterator()
        var last = iterator.next()
        for (item in iterator) {
          if (item < last) return false
          last = item
        }
        return true
      }
    }

  @Suppress("NOTHING_TO_INLINE") // using inline to never let KProperty0 leave the caller’s scope; see checkInvariant.kt
  internal inline infix fun KProperty0<List<*>>.hasSameSizeAs(other: KProperty0<List<*>>) = checkInvariant(with = other, ::haveSameSize)

  internal fun haveSameSize(
    leftCollection: List<*>,
    rightCollection: BooleanArray,
  ) = leftCollection.size == rightCollection.size

  @JvmName("hasSameSizeAsBooleanArray")
  @Suppress("NOTHING_TO_INLINE") // using inline to never let KProperty0 leave the caller’s scope; see checkInvariant.kt
  internal inline infix fun KProperty0<List<*>>.hasSameSizeAs(other: KProperty0<BooleanArray>) =
    checkInvariant(with = other, ::haveSameSize)

  internal fun haveSameSize(
    leftCollection: List<*>,
    rightCollection: List<*>,
  ) = leftCollection.size == rightCollection.size
}
