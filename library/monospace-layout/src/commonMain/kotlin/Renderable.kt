package software.consistent.model.formatting.monospacelayout

internal fun interface Renderable {
  context(layout: LayoutContext)
  fun renderTo(target: LayoutTarget)
}
