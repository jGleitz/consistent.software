package software.consistent.model.type

import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.property.Arb
import io.kotest.property.checkAll
import software.consistent.model.generators.value

class JustSpec : DescribeSpec({
  describe(Just::class.simpleName!!) {
    it("contains the specified value") {
      checkAll(Arb.value()) { value ->
        expect(Just(value)).toContain(value)
      }
    }

    it("contains no value that isn’t the specified one") {
      checkAll(Arb.value(), Arb.value()) { specifiedValue, testValue ->
        //assume(specifiedValue != testValue)
        expect(Just(specifiedValue)).notToContain(testValue)
      }
    }
  }
})
