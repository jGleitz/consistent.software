package software.consistent.model.formatting.monospacelayout

internal data class LayoutContext(val settings: LayoutSettings, val state: LineState = LineState()) {
  internal data class LineState(var position: Int = 0, var boxesEntered: Int = 0, var indent: StringBuilder = StringBuilder())
}

