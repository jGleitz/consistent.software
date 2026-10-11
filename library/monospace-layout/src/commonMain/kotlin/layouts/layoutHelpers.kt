package software.consistent.data.formatting.monospacelayout.layouts

import software.consistent.data.formatting.monospacelayout.LayoutContext
import software.consistent.data.formatting.monospacelayout.PrintWidthIfSupported
import software.consistent.data.formatting.monospacelayout.PrintWidthIfSupported.Companion.supported

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
