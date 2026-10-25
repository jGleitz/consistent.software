package software.consistent.data.generators

import io.kotest.property.Exhaustive
import io.kotest.property.exhaustive.exhaustive

/** All [UByte]s from [from] to [to]; the unsigned counterpart of [io.kotest.property.exhaustive.bytes]. */
internal fun Exhaustive.Companion.uBytes(from: UByte = UByte.MIN_VALUE, to: UByte = UByte.MAX_VALUE): Exhaustive<UByte> =
  (from..to).map { it.toUByte() }.exhaustive()
