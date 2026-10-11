package software.consistent.model.type

import software.consistent.model.formatting.monospacelayout.WithMonospaceLayout

internal val Type.monospaceLayout
  get() = when (this) {
    is WithMonospaceLayout -> monospaceLayout
  }
