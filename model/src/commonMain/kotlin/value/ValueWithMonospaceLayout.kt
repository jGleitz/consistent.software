package software.consistent.model.value

import software.consistent.model.formatting.monospacelayout.WithMonospaceLayout

internal val Value.monoSpaceLayout
  get() = when (this) {
    is WithMonospaceLayout -> monospaceLayout
  }
