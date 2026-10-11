package software.consistent.data.formatting.monospacelayout

internal fun interface Renderable {
  context(layout: LayoutContext)
  fun renderTo(target: LayoutTarget)
}
