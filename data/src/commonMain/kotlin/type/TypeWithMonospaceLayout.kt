package software.consistent.data.type

import software.consistent.data.formatting.monospacelayout.WithMonospaceLayout

internal val Type.monospaceLayout
  get() =
    when (this) {
      is WithMonospaceLayout -> monospaceLayout
    }
