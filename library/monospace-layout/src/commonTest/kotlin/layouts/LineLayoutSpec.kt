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

    it("prints all items in a single line if they fit exactly") {
      val line = Monospace.line(separator = "|", size = 3) {
        -"a"
        -"b"
        -"c"
      }
      expect(line.renderToString(LayoutSettings(breakLineAfterColumn = 9))).toEqual("a | b | c")
    }

    it("breaks all items if only the last item exceeds the available width") {
      val line = Monospace.line(separator = "|", size = 3) {
        -"a"
        -"b"
        -"c"
      }
      expect(line.renderToString(LayoutSettings(breakLineAfterColumn = 8, indent = "  "))).toEqual(
        """
          a
            | b
            | c
        """.trimIndent()
      )
    }

    it("breaks all items if their count exceeds maxItemsPerLine") {
      val line = Monospace.line(separator = "|", size = 3) {
        -"a"
        -"b"
        -"c"
      }
      expect(line.renderToString(LayoutSettings(maxItemsPerLine = 3))).toEqual("a | b | c")
      expect(line.renderToString(LayoutSettings(maxItemsPerLine = 2, indent = "  "))).toEqual("a\n  | b\n  | c")
    }

    it("uses the content's print width and the appropriate separator") {
      val line = Monospace.line(separator = Separator(singleLine = "--", multiLine = "> "), size = 3) {
        -"界"
        -"b"
        -"c"
      }
      expect(line.renderToString(LayoutSettings(breakLineAfterColumn = 8))).toEqual("界--b--c")
      expect(line.renderToString(LayoutSettings(breakLineAfterColumn = 7, indent = "  "))).toEqual("界\n  > b\n  > c")
    }

    it("breaks only the outer line if nested lines still fit") {
      val line = Monospace.line(separator = "&!", size = 2) {
        -Monospace.line(separator = "&", size = 2) {
          -"a"
          -"b"
        }.containerWrappedIn("(", ")")
        -Monospace.line(separator = "&", size = 2) {
          -"c"
          -"d"
        }.containerWrappedIn("(", ")")
      }
      expect(line.renderToString(LayoutSettings(breakLineAfterColumn = 18))).toEqual("(a & b) &! (c & d)")
      expect(line.renderToString(LayoutSettings(breakLineAfterColumn = 17, indent = "  "))).toEqual("(a & b)\n  &! (c & d)")
      expect(line.renderToString(LayoutSettings(breakLineAfterColumn = 11, indent = "  "))).toEqual(
        """
          (a & b)
            &! (c
              & d)
        """.trimIndent()
      )
    }

    it("breaks all items if the last item cannot be printed in a single line") {
      val line = Monospace.line(separator = "|", size = 3) {
        -"a"
        -"b"
        -Monospace.list(prefix = "[", postfix = "]", separator = ",", size = 4) {
          -"x"
          -"y"
          -"z"
          -"w"
        }
      }
      expect(line.renderToString(LayoutSettings(maxItemsPerLine = 3, indent = "  "))).toEqual(
        """
          a
            | b
            | [
              x,
              y,
              z,
              w
            ]
        """.trimIndent()
      )
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
