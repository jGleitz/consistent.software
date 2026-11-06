package software.consistent.data.type

import ch.tutteli.atrium.api.verbs.expect
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.property.Arb
import io.kotest.property.checkAll
import software.consistent.data.generators.value

class JustSpec :
  DescribeSpec({
    describe(Just::class.simpleName!!) {
      // TODO this test is valid but its required code is not finished
      it("!contains the specified value") {
        checkAll(Arb.value()) { value ->
          expect(Just(value)).toAllow(value)
        }
      }

      // TODO this test is valid but its required code is not finished
      it("!contains no value that isn’t the specified one") {
        checkAll(Arb.value(), Arb.value()) { specifiedValue, testValue ->
          // assume(specifiedValue != testValue)
          expect(Just(specifiedValue)).toForbid(testValue)
        }
      }
    }
  })
