package software.consistent.collections

import ch.tutteli.atrium.api.fluent.en_GB.*
import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec

class TestReportDemoJvmSpec : DescribeSpec({
  describe("test report demo") {
    it("fails only on Linux") {
      expect(System.getProperty("os.name")).notToContain("Linux")
    }
  }
})
