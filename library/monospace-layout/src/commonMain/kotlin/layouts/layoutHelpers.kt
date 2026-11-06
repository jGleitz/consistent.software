package software.consistent.formatting.monospacelayout.layouts

import software.consistent.formatting.monospacelayout.LayoutContext
import software.consistent.formatting.monospacelayout.PrintWidthIfSupported
import software.consistent.formatting.monospacelayout.PrintWidthIfSupported.Companion.supported

context(layout: LayoutContext)
internal inline fun hypotheticalWidth(block: () -> Unit): PrintWidthIfSupported {
  val startRemaining = layout.state.remainingWidthBeforeLineBreak
  try {
    block()
    return supported(startRemaining - layout.state.remainingWidthBeforeLineBreak)
  } finally {
    layout.state.remainingWidthBeforeLineBreak = startRemaining
  }
}
