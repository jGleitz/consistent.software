package software.consistent.collections

import ch.tutteli.atrium.api.fluent.en_GB.*
import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec

class TestReportDemoJsSpec : DescribeSpec({
  describe("test report demo") {
    it("fails only on Kotlin/JS") {
      expect("js").toEqual("jvm")
    }
  }
})
