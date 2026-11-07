package software.consistent.data.type.solvers

import kotlin.jvm.JvmInline
import kotlin.math.abs

@JvmInline
internal value class Cardinality(
  // Int.MIN_VALUE: more than Int.MAX_VALUE
  // negative: at least (-value), might be higher
  // positive: exactly value
  private val value: Int
) {
  companion object {
    fun moreThanIntMaxValue() = Cardinality(Int.MIN_VALUE)
    fun atLeast(value: Int): Cardinality {
      require(value > 0) {
        // TODO better exception?
        "Argument to ${::atLeast.name} must be positive (got $value)"
      }
      return Cardinality(-value)
    }

    fun exactly(value: Int): Cardinality {
      require(value >= 0) {
        // TODO better exception?
        "Argument to ${::exactly.name} must not be negative (got $value)"
      }
      return Cardinality(value)
    }
  }

  fun isMoreThanIntMaxValue() = value == Int.MIN_VALUE
  fun isLowerBound() = value < 0
  fun isExact() = value >= 0

  val lowerBound: Int
    get() = when {
      isMoreThanIntMaxValue() -> MAX_VALUE
      isLowerBound() -> -value
      else -> value
    }

  fun asLowerBound() = if (isExact()) atLeast(value) else this

  val exactValue: Int
    get() {
      require(value >= 0) { "Not an exact cardinality: $this" }
      return value
    }

  operator fun plus(other: Cardinality) = when {
    this.isMoreThanIntMaxValue() || other.isMoreThanIntMaxValue() -> moreThanIntMaxValue()
    this.isLowerBound() || other.isLowerBound() -> {
      val sum = abs(this.value) + abs(other.value)
      if (sum >= 0) atLeast(sum) else /* overflow */ moreThanIntMaxValue()
    }

    else -> {
      val sum = this.value + other.value
      if (sum >= 0) exactly(sum) else /* overflow */ moreThanIntMaxValue()
    }
  }

  operator fun times(other: Cardinality) = when {
    this.value == 0 || other.value == 0 -> exactly(0)
    this.isMoreThanIntMaxValue() || other.isMoreThanIntMaxValue() -> moreThanIntMaxValue()
    this.isLowerBound() || other.isLowerBound() -> {
      val product = abs(this.value.toLong()) * abs(other.value.toLong())
      if (product > Int.MAX_VALUE.toLong()) moreThanIntMaxValue() else atLeast(product.toInt())
    }

    else -> {
      val product = this.value.toLong() * other.value.toLong()
      if (product > Int.MAX_VALUE.toLong()) moreThanIntMaxValue() else exactly(product.toInt())
    }
  }

  infix fun toThePowerOf(other: Cardinality): Cardinality = when {
    this.value == 0 -> exactly(0)
    other.value == 0 -> exactly(1)
    this.isMoreThanIntMaxValue() || other.isMoreThanIntMaxValue() -> moreThanIntMaxValue()
    this.isLowerBound() || other.isLowerBound() -> cardinalityPower(abs(this.value.toLong()), abs(other.value.toLong()), ::atLeast)
    else -> cardinalityPower(this.value.toLong(), other.value.toLong(), ::exactly)
  }

  private inline fun cardinalityPower(base: Long, exponent: Long, cardinalityConstructor: (Int) -> Cardinality): Cardinality {
    var result = 1L
    var factor = base
    var remainingExponent = exponent
    while (remainingExponent > 0) {
      if (remainingExponent and 1L != 0L) {
        result *= factor
      }
      if (result > Int.MAX_VALUE.toLong()) {
        return moreThanIntMaxValue()
      }
      remainingExponent = remainingExponent ushr 1
      if (remainingExponent > 0) {
        factor *= factor
        if (factor > Int.MAX_VALUE.toLong()) {
          return moreThanIntMaxValue()
        }
      }
    }
    return cardinalityConstructor(result.toInt())
  }

  operator fun compareTo(otherValue: Int): Int {
    require(otherValue >= 0) {
      // TODO better exception?
      "Argument to ${::compareTo.name} must not be negative (got $otherValue)"
    }
    return when {
      this.isMoreThanIntMaxValue() -> 1
      this.isExact() -> this.value - otherValue
      else -> {
        val absValue = -value
        require(otherValue < absValue) {
          // TODO better exception?
          "This cardinality (at least $absValue) can only be compared to values up to ${absValue - 1} (got $otherValue)"
        }
        absValue - otherValue
      }
    }
  }
}
