package software.consistent.data.generators

import io.kotest.property.*
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.removeEdgecases
import software.consistent.data.generators.ValueArb.number
import software.consistent.data.generators.ValueArb.symbol
import software.consistent.data.value.Value
import kotlin.random.nextInt

internal class MapArb private constructor(
  private val innerValueArbsByMaxNesting: Array<Arb<Value>>,
  private val size: IntRange,
  private val maxNesting: Int,
  private val slippage: Int = 10
) : Arb<Value.Map>() {
  init {
    require(!size.isEmpty()) { "size must not be empty! (was $size)" }
    require(size.first >= 0) { "size must not be negative" }
    require(maxNesting >= 1) { "must allow nesting at least once for a Map (was $maxNesting)" }
    require(innerValueArbsByMaxNesting.size >= maxNesting - 1) {
      "innerValueArbsByMaxNesting must have at least size ${maxNesting - 1} (was ${innerValueArbsByMaxNesting.size})"
    }
  }

  override fun edgecase(rs: RandomSource) = Value.Map().asSample().takeIf { 0 in size }

  override fun sample(rs: RandomSource): Sample<Value.Map> {
    val targetSize = rs.random.nextInt(size)
    if (targetSize == 0) return Value.Map().asSample()

    val maxKeyNesting = rs.random.nextInt(until = maxNesting)
    val maxValueNesting = rs.random.nextInt(until = maxNesting)
    val maxMisses = targetSize * slippage
    val pairArb = Arb.pair(
      innerValueArbsByMaxNesting[maxKeyNesting],
      innerValueArbsByMaxNesting[maxValueNesting]
    )

    return Arb.bind(List(targetSize) { pairArb }) { it.toMap() }
      .samples(rs)
      .take(maxMisses)
      .firstOrNull { it.value.size == targetSize }
      // removing key entries is much more likely to converge fast, so we try it before shrinking the inner values
      ?.recursivelyPrependShrinker(this::standardMapShrink)
      ?.map { Value.Map(it) }
      ?: throw IllegalStateException("the allowed size requirement of $size could not be satisfied within $maxMisses tries")
  }

  private fun standardMapShrink(previousValue: Map<Value, Value>) = listOf(
    previousValue.toList().take(previousValue.size / 2),
    previousValue.toList().drop(1)
  )
    .filter { it.size in size }
    .map { it.toMap() }

  internal companion object {
    // TODO self-referencing maps?
    internal operator fun invoke(structureSize: IntRange = 0..10, maxNesting: Int = 12): Arb<Value.Map> {
      val primitiveValueArbs = arrayOf(symbol(), number())
      val innerValueArbsByMaxNesting = Array<Arb<Value>>(maxNesting) { /* cheap non-null filler */ symbol() }
      innerValueArbsByMaxNesting[0] = Choice(*primitiveValueArbs)
      for (lowerMaxNesting in 1 until maxNesting) {
        val mapArb = MapArb(
          innerValueArbsByMaxNesting,
          maxNesting = lowerMaxNesting,
          size = structureSize
        )
        innerValueArbsByMaxNesting[lowerMaxNesting] = Choice(*primitiveValueArbs, mapArb)
      }

      return MapArb(innerValueArbsByMaxNesting, structureSize, maxNesting)
    }
  }
}

private fun <A> Sample<A>.recursivelyPrependShrinker(shrinker: Shrinker<A>) = Sample(value, shrinks.recursivelyPrependShrinker(shrinker))

private fun <A> RTree<A>.recursivelyPrependShrinker(shrinker: Shrinker<A>): RTree<A> = RTree(value, lazy {
  val originalChildren by this.children
  val addedChildren by shrinker.rtree(value).children
  (originalChildren + addedChildren).map { it.recursivelyPrependShrinker(shrinker) }
})


/**
 * Implementation of [Arb.pair] that uses [Choice] to retain shrinks.
 */
// TODO remove once [Arb.choice] is fixed upstream
private fun <K, V> Arb.Companion.pair(k: Arb<K>, v: Arb<V>): Arb<Pair<K, V>> {
  val arbPairWithoutKeyEdges: Arb<Pair<K, V>> = Arb.bind(k.removeEdgecases(), v, ::Pair)
  val arbPairWithoutValueEdges: Arb<Pair<K, V>> = Arb.bind(k, v.removeEdgecases(), ::Pair)
  val arbPair: Arb<Pair<K, V>> = Arb.bind(k, v, ::Pair)
  return Choice(arbPair, arbPairWithoutKeyEdges, arbPairWithoutValueEdges)
}

private fun <A, B> Sample<A>.map(fn: (A) -> B): Sample<B> = Sample(fn(value), shrinks.map(fn))
