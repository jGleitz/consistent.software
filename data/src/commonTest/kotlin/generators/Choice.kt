package software.consistent.data.generators

import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.Sample
import io.kotest.property.arbitrary.edgecase

/**
 * Implementation of [io.kotest.property.arbitrary.choice] that doesn’t discard shrinks.
 */
// TODO contribute fix upstream and remove from here
internal class Choice<A>(private val arbs: List<Arb<A>>) : Arb<A>() {
  constructor(vararg arbs: Arb<A>) : this(arbs.toList())

  override fun edgecase(rs: RandomSource) = arbs.edgecase(rs)

  override fun sample(rs: RandomSource): Sample<A> = arbs.random(rs.random).sample(rs)
  /* TODO
    override val classifier
      get() = Classifier<A> { value ->
        arbs.map { it.classifier?.classify(value) }.firstOrNull()
      }

   */
}
