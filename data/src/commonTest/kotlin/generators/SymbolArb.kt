package software.consistent.data.generators

import io.kotest.property.Arb
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.plusEdgecases
import io.kotest.property.arbs.movies.harryPotterCharacter
import io.kotest.property.arbs.movies.movieStar
import software.consistent.data.value.FALSE
import software.consistent.data.value.TRUE
import software.consistent.data.value.Value

internal object SymbolArb {
  private val instance =
    Choice(
      Arb.harryPotterCharacter().map { "${it.firstName}_${it.lastName}" },
      Arb.movieStar().map { it.name },
    ).map { Value.Symbol(it.replace(Regex("[ ']"), "_")) }
      .plusEdgecases(listOf(TRUE, FALSE))

  internal operator fun invoke() = instance
}
