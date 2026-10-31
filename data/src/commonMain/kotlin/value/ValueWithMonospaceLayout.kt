package software.consistent.data.value

import software.consistent.data.formatting.monospacelayout.WithMonospaceLayout

internal val Value.monospaceLayout
  get() =
    when (this) {
      is WithMonospaceLayout -> monospaceLayout
    }
