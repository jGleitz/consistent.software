package software.consistent.data.formatting.monospacelayout

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.fluent.en_GB.toThrow
import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec
import software.consistent.data.formatting.monospacelayout.layouts.MonospaceLayout

class MonospaceAtomSpec :
  DescribeSpec({
    describe("Monospace.atom") {
      val variants =
        listOf<Pair<String, (CharSequence) -> MonospaceLayout>>(
          "with content" to { content -> Monospace.atom(content) },
          "with a builder" to { content -> Monospace.atom { append(content) } },
          "with an initial capacity and a builder" to { content -> Monospace.atom(expectedSize = 1) { append(content) } },
        )

      for ((name, atom) in variants) {
        describe(name) {
          it("prints character sequences verbatim") {
            expect(atom("test text 界").renderToString()).toEqual("test text 界")
            expect(atom(StringBuilder("test text 界")).renderToString()).toEqual("test text 界")
          }

          it("prints empty content as an empty atom") {
            expect(atom("").renderToString()).toEqual("")
          }

          it("prints in a single line even when the content exceeds the configured width") {
            expect(atom("test text").renderToString(LayoutSettings(breakLineAfterColumn = 4))).toEqual("test text")
          }

          it("rejects newlines") {
            for (newline in listOf("\n", "\r", "\r\n")) {
              expect { atom("a${newline}b") }.toThrow<IllegalArgumentException>()
            }
          }
        }
      }

      it("prints appended characters and character sequences verbatim") {
        val atom =
          Monospace.atom {
            append('a').append("bc").append(StringBuilder("界"))
          }
        expect(atom.renderToString()).toEqual("abc界")
      }

      it("prints appended content verbatim with an initial capacity smaller than the content") {
        val atom =
          Monospace.atom(expectedSize = 1) {
            append('a').append("bc").append(StringBuilder("界"))
          }
        expect(atom.renderToString()).toEqual("abc界")
      }

      it("prints appended layouts between other content") {
        val list = Monospace.list(listOf(Monospace.atom("a"), Monospace.atom("b")), "[", "]", ",")
        val atom =
          Monospace.atom {
            append("before ").append(list).append(" after")
          }
        expect(atom.renderToString()).toEqual("before [a, b] after")
      }
    }
  })
