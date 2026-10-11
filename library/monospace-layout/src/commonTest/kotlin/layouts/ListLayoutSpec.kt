package software.consistent.data.formatting.monospacelayout

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec

class ListLayoutSpec : DescribeSpec({
  describe("Mono.list") {
    it("uses the content's print width when deciding whether a containing list fits") {
      val list = Monospace.list(
        listOf(Monospace.atom { append("界") }, Monospace.atom(expectedSize = 1) { append('a') }),
        "[", "]", ","
      )
      expect(list.renderToString(LayoutSettings(breakLineAfterColumn = 7))).toEqual("[界, a]")
      expect(list.renderToString(LayoutSettings(breakLineAfterColumn = 6, indent = "  "))).toEqual("[\n  界,\n  a\n]")
    }

    it("prints an empty list in a single line") {
      val list = Monospace.list(prefix = "[", postfix = "]", Separator(singleLine = ",", multiLine = "-"), size = 0) {}
      expect(list.renderToString()).toEqual("[]")
    }

    it("prints a list in a single line if it fits") {
      val list =
        Monospace.list(prefix = "[", postfix = "]", Separator(singleLine = ",-", multiLine = ","), size = 3) {
          -"a"
          -"b"
          -"c"
        }
      val settings = LayoutSettings(breakLineAfterColumn = 9)
      expect(list.renderToString(settings)).toEqual("[a,-b,-c]")
    }

    it("prints a list in multiple lines if it exceeds breakLineAfterLength") {
      val list =
        Monospace.list(prefix = "[", postfix = "]", Separator(singleLine = ",-", multiLine = ","), size = 3) {
          -"a"
          -"b"
          -"c"
        }
      val settings = LayoutSettings(breakLineAfterColumn = 8, indent = "  ")
      expect(list.renderToString(settings)).toEqual(
        """
            [
              a,
              b,
              c
            ]
          """.trimIndent()
      )
    }

    it("prints nested lists in a single line if they fit") {
      val list =
        Monospace.list(prefix = "[", postfix = "]", separator = ",", size = 5) {
          -"before"
          -Monospace.list(prefix = "{", postfix = "}", separator = ";", size = 3) {
            -"a"
            -"b"
            -"c"
          }
          -"between"
          -Monospace.list(prefix = "<", postfix = ">", Separator(singleLine = "--", multiLine = ","), size = 3) {
            -"123"
            -"456"
            -"789"
          }
          -"after"
        }
      val settings = LayoutSettings(breakLineAfterColumn = 52)
      expect(list.renderToString(settings)).toEqual("[before, {a; b; c}, between, <123--456--789>, after]")
    }

    it("breaks only the outer list if exceeding breakLineAfterLength by a little") {
      val list =
        Monospace.list(prefix = "[", postfix = "]", separator = ",", size = 5) {
          -"before"
          -Monospace.list(prefix = "{", postfix = "}", separator = ";", size = 3) {
            -"a"
            -"b"
            -"c"
          }
          -"between"
          -Monospace.list(prefix = "<", postfix = ">", Separator(singleLine = "--", multiLine = ","), size = 3) {
            -"123"
            -"456"
            -"789"
          }
          -"after"
        }
      val settings = LayoutSettings(breakLineAfterColumn = 51, indent = "  ")
      expect(list.renderToString(settings)).toEqual(
        """
            [
              before,
              {a; b; c},
              between,
              <123--456--789>,
              after
            ]""".trimIndent()
      )
    }

    it("includes the outer multiLine separator when deciding whether inner lists fit on a single line") {
      // 2 columns of indentation + 8 for {ab; cd} + 2 for the outer multiLine separator = exactly 12.
      // {abc; cd} exceeds that limit by one; both inner lists need the following outer separator.
      val settings = LayoutSettings(breakLineAfterColumn = 12, indent = "  ")
      for (singleLineSeparator in listOf(",", ",--")) {
        // Using the shorter singleLine separator would keep both inner lists on one line;
        // using the longer one would break both, instead of keeping only the exact fit.
        val list =
          Monospace.list(
            prefix = "[", postfix = "]",
            separator = Separator(singleLine = singleLineSeparator, multiLine = ",-"), size = 3
          ) {
            -Monospace.list(prefix = "{", postfix = "}", separator = ";", size = 2) {
              -"ab"
              -"cd"
            }
            -Monospace.list(prefix = "{", postfix = "}", separator = ";", size = 2) {
              -"abc"
              -"cd"
            }
            -"after"
          }
        expect(list.renderToString(settings)).toEqual(
          """
            [
              {ab; cd},-
              {
                abc;
                cd
              },-
              after
            ]
          """.trimIndent()
        )
      }
    }

    it("breaks only the required lists when exceeding breakLineAfterLength") {
      val list =
        Monospace.list(prefix = "[", postfix = "]", separator = ",", size = 5) {
          -"before"
          -Monospace.list(prefix = "{", postfix = "}", separator = ";", size = 3) {
            -"a"
            -"b"
            -"c"
          }
          -"between"
          -Monospace.list(prefix = "<", postfix = ">", "--", size = 3) {
            -"123"
            -"456"
            -"789"
          }
          -"after"
        }
      val settings = LayoutSettings(breakLineAfterColumn = 16, indent = "  ")
      expect(list.renderToString(settings)).toEqual(
        """
            [
              before,
              {a; b; c},
              between,
              <
                123--
                456--
                789
              >,
              after
            ]""".trimIndent()
      )
    }

    it("breaks all lists if breakLineAfterLength requires it") {
      val list =
        Monospace.list(prefix = "[", postfix = "]", separator = ",", size = 5) {
          -"before"
          -Monospace.list(prefix = "{", postfix = "}", separator = ";", size = 3) {
            -"a"
            -"b"
            -"c"
          }
          -"between"
          -Monospace.list(prefix = "<", postfix = ">", "--", size = 3) {
            -"123"
            -"456"
            -"789"
          }
          -"after"
        }
      val settings = LayoutSettings(breakLineAfterColumn = 10, indent = "  ")
      expect(list.renderToString(settings)).toEqual(
        """
            [
              before,
              {
                a;
                b;
                c
              },
              between,
              <
                123--
                456--
                789
              >,
              after
            ]""".trimIndent()
      )
    }
  }
})
