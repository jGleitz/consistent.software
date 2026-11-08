package software.consistent.data.type.solvers.maps

import kotlin.jvm.JvmInline

@JvmInline
internal value class SearchOutcome<T : Any>(val result: T?) {
  inline fun elseTry(block: () -> SearchOutcome<T>): SearchOutcome<T> =
      result?.let { SearchOutcome(it) } ?: block()

  inline fun orElse(fallback: () -> T): T = result ?: fallback()

  fun orElseNull(): T? = result

  fun isFound(): Boolean = result != null

  internal companion object {
    internal fun <T : Any> stopAndReturn(value: T): SearchOutcome<T> = SearchOutcome(value)

    internal fun <T : Any> continueSearching(): SearchOutcome<T> = SearchOutcome(null)

    internal fun <T : Any> notFound(): SearchOutcome<T> = SearchOutcome(null)
  }
}
