package software.consistent.collections

import ch.tutteli.atrium.api.fluent.en_GB.*
import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec

class TestReportDemoSpec : DescribeSpec({
  describe("test report demo") {
    it("fails on every operating system and Kotlin target") {
      expect(1 + 1).toEqual(3)
    }
  }
})
