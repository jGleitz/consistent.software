package software.consistent.data.formatting.monospacelayout

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec

class LineLayoutSpec : DescribeSpec({
  describe("Monospace.line") {
    it("prints an empty line with zero available width") {
      expect(Monospace.line("-", emptyList()).renderToString(LayoutSettings(breakLineAfterColumn = 0))).toEqual("")
    }

    it("uses the single-line width to decide whether a line fits") {
      val line = Monospace.line("-", listOf(Monospace.atom("a"), Monospace.atom("b")))
      expect(line.renderToString(LayoutSettings(breakLineAfterColumn = 5))).toEqual("a - b")
      expect(line.renderToString(LayoutSettings(breakLineAfterColumn = 4, indent = "  "))).toEqual("a\n  - b")
    }

    it("propagates unsupported single-line layouts through wrappers") {
      val inner = Monospace.list(listOf(Monospace.atom("a"), Monospace.atom("b")), "[", "]", ",")
        .wrappedIn("(", ")")
      val line = Monospace.line("-", listOf(inner))
      expect(line.renderToString(LayoutSettings(maxItemsPerLine = 1, indent = "  "))).toEqual("([\n  a,\n  b\n])")
    }

    it("includes a wrapper's prefix and postfix in the supported width") {
      val line = Monospace.line("-", listOf(Monospace.atom("a"), Monospace.atom("b"))).wrappedIn("[", "]")
      expect(line.renderToString(LayoutSettings(breakLineAfterColumn = 7))).toEqual("[a - b]")
      expect(line.renderToString(LayoutSettings(breakLineAfterColumn = 6, indent = "  "))).toEqual("[a\n  - b]")
    }
  }
})
