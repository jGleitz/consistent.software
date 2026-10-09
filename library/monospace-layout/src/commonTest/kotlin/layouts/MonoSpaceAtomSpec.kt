package software.consistent.model.formatting.monospacelayout.layouts

import ch.tutteli.atrium.api.fluent.en_GB.toEqual
import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec
import software.consistent.model.formatting.monospacelayout.LayoutSettings
import software.consistent.model.formatting.monospacelayout.Monospace

class MonoSpaceAtomSpec : DescribeSpec({
  describe("Mono.atom") {
    it("prints an atom in a single line") {
      val layout = Monospace.atom("test text").renderToString()
      expect(layout).toEqual("test text")
    }

    it("prints an atom in a single line even if it exceeds breakLineAfterLength") {
      val layout = Monospace.atom("test text").renderToString(LayoutSettings(breakLineAfterLength = 4))
      expect(layout).toEqual("test text")
    }
  }
})
