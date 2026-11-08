package software.consistent.data.type

import software.consistent.formatting.monospacelayout.WithMonospaceLayout

internal val Type.monospaceLayout
  get() =
      when (this) {
        is WithMonospaceLayout -> monospaceLayout
      }
