package software.consistent.data.generators

import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import software.consistent.data.value.Value
import kotlin.math.*

internal object NumberArb {
  private val fractions =
    Arb.bind(
      Arb.longEqualMagnitudeProbability(absMax = 999_999),
      Arb.nonNegativeLongEqualMagnitudeProbability(min = 1, max = 999),
    ) { numerator, denominator ->
      Value.Number(numerator) / Value.Number(denominator)
    }

  private val wholeNumbers = Arb.longEqualMagnitudeProbability(absMax = 999_999_999).map { Value.Number(it) }

  internal operator fun invoke(): Arb<Value.Number> = Choice(fractions, wholeNumbers)
}

private fun Arb.Companion.longEqualMagnitudeProbability(
  absMin: Long = 0,
  absMax: Long = Long.MAX_VALUE,
): Arb<Long> {
  val nonNegativeGenerator = Arb.nonNegativeLongEqualMagnitudeProbability(absMin, absMax)
  return Choice(nonNegativeGenerator.map { -it }, nonNegativeGenerator)
}

private fun Arb.Companion.nonNegativeLongEqualMagnitudeProbability(
  min: Long = 0,
  max: Long = Long.MAX_VALUE,
): Arb<Long> {
  require(min >= 0) { "min must be non-negative! (was $min)" }
  require(min < 100L) { "min must not exceed 100! (was $min)" }
  require(max > min) { "max must be higher than min! (min was $min, max was $max)" }

  val maxMagnitude = max(ceil(log10(max.toDouble())).toInt(), 0)
  return Choice(
    Arb.long(min..100),
    *Array(maxMagnitude - 2) { index ->
      val magnitude = index + 2
      Arb.long(min = 10.0.pow(magnitude).roundToLong() + 1, max = min(max, 10.0.pow(magnitude + 1).roundToLong()))
    },
  )
}
