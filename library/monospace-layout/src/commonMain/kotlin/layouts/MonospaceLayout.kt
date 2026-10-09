package software.consistent.model.formatting.monospacelayout.layouts

import software.consistent.model.formatting.monospacelayout.LayoutSettings

public sealed interface MonospaceLayout {
  public fun renderToString(settings: LayoutSettings = LayoutSettings()): String
}
