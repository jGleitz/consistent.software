package software.consistent.data.value

import software.consistent.formatting.monospacelayout.WithMonospaceLayout

internal val Value.monospaceLayout
  get() =
      when (this) {
        is WithMonospaceLayout -> monospaceLayout
      }
