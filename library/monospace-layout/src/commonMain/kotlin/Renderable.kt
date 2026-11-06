package software.consistent.formatting.monospacelayout

internal fun interface Renderable {
  context(layout: LayoutContext)
  fun renderTo(target: LayoutTarget)
}
