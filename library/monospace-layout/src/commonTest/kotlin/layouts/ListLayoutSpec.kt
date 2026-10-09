package software.consistent.model.formatting.monospacelayout

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec

class ListLayoutSpec : DescribeSpec({
  describe("Mono.list") {
    it("prints an empty list in a single line") {
      val list = Monospace.list(prefix = "[", postfix = "]", Separator(",", appendIfSingleLine = "-"), size = 0) {}
      expect(list.renderToString()).toEqual("[]")
    }

    it("prints a list in a single line if it fits") {
      val list =
        Monospace.list(prefix = "[", postfix = "]", Separator(",", appendIfSingleLine = "-"), size = 3) {
          -"a"
          -"b"
          -"c"
        }
      val settings = LayoutSettings(breakLineAfterLength = 9)
      expect(list.renderToString(settings)).toEqual("[a,-b,-c]")
    }

    it("prints a list in multiple lines if it exceeds breakLineAfterLength") {
      val list =
        Monospace.list(prefix = "[", postfix = "]", Separator(",", appendIfSingleLine = "-"), size = 3) {
          -"a"
          -"b"
          -"c"
        }
      val settings = LayoutSettings(breakLineAfterLength = 8, indent = "  ")
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
          -Monospace.list(prefix = "<", postfix = ">", Separator("--", appendIfSingleLine = ""), size = 3) {
            -"123"
            -"456"
            -"789"
          }
          -"after"
        }
      val settings = LayoutSettings(breakLineAfterLength = 52)
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
          -Monospace.list(prefix = "<", postfix = ">", Separator("--", appendIfSingleLine = ""), size = 3) {
            -"123"
            -"456"
            -"789"
          }
          -"after"
        }
      val settings = LayoutSettings(breakLineAfterLength = 51, indent = "  ")
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
        -Monospace.list(prefix = "<", postfix = ">", Separator("--", appendIfSingleLine = ""), size = 3) {
          -"123"
          -"456"
          -"789"
        }
        -"after"
      }
    val settings = LayoutSettings(breakLineAfterLength = 16, indent = "  ")
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
        -Monospace.list(prefix = "<", postfix = ">", Separator("--", appendIfSingleLine = ""), size = 3) {
          -"123"
          -"456"
          -"789"
        }
        -"after"
      }
    val settings = LayoutSettings(breakLineAfterLength = 10, indent = "  ")
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

  it("inserts no newline for prefix or postfix if they are null") {
    val list =
      Monospace.list(prefix = "[", postfix = "]", separator = ",", size = 1) {
        -Monospace.list(separator = ";;", size = 3) {
          -"a"
          -"b"
          -"c"
        }
      }
    val settings = LayoutSettings(breakLineAfterLength = 10, indent = "  ")
    expect(list.renderToString(settings)).toEqual(
      """
            [
              a;;
              b;;
              c
            ]""".trimIndent()
    )
  }
})
