// we can’t refer to `this` in kotlin’s js function, so we rename `this` to `it` using `let`
@file:Suppress("ComplexRedundantLet")

package software.consistent.exactnumbers

import software.consistent.exactnumbers.BigIntegers.ZERO

public external class JsNativeBigInt private constructor()

@JsName("BigInt")
private external fun nativeBigInt(int: Int): JsNativeBigInt

@JsName("BigInt")
private external fun nativeBigInt(decimal: String): JsNativeBigInt

public actual typealias BigInteger = JsNativeBigInt

public actual fun Byte.toBigInteger(): BigInteger = nativeBigInt(toInt())
public actual fun UByte.toBigInteger(): BigInteger = nativeBigInt(toInt())
public actual fun Short.toBigInteger(): BigInteger = nativeBigInt(toInt())
public actual fun UShort.toBigInteger(): BigInteger = nativeBigInt(toInt())
public actual fun Int.toBigInteger(): BigInteger = nativeBigInt(this)

// TODO these values _are_ alreay JS BigInts, there must be a better way than going via toString()
public actual fun UInt.toBigInteger(): BigInteger = nativeBigInt(toString())
public actual fun Long.toBigInteger(): BigInteger = nativeBigInt(toString())
public actual fun ULong.toBigInteger(): BigInteger = nativeBigInt(toString())

internal fun BigInteger.asInt(): Int = let { js(/* language=JavaScript */ "return Number(it)") }

public actual val BigInteger.absoluteValue: BigInteger get() = let { js(/* language=JavaScript */ "it < BigInt(0) ? -it : it") }
public actual operator fun BigInteger.unaryMinus(): BigInteger = let { js(/* language=JavaScript */ "-it") }

public actual operator fun BigInteger.plus(addend: BigInteger): BigInteger = let { js(/* language=JavaScript */ "it + addend") }
public actual operator fun BigInteger.minus(subtrahend: BigInteger): BigInteger = let { js(/* language=JavaScript */ "it - subtrahend") }
public actual operator fun BigInteger.times(factor: BigInteger): BigInteger = let { js(/* language=JavaScript */ "it * factor") }
public actual inline fun <R> BigInteger.divideAndRemainder(
  divisor: BigInteger,
  resultFn: (quotient: BigInteger, remainder: BigInteger) -> R
): R {
  if (divisor == ZERO) throw divisionByZero(this)
  return let { resultFn(js(/* language=JavaScript */ "it / divisor"), js(/* language=JavaScript */ "it % divisor")) }
}

public actual infix fun BigInteger.shl(bits: Int): BigInteger = let { js(/* language=JavaScript */ "it << BigInt(bits)") }
public actual infix fun BigInteger.shr(bits: Int): BigInteger = let { js(/* language=JavaScript */ "it >> BigInt(bits)") }

public actual operator fun BigInteger.compareTo(other: BigInteger): Int =
  let { js(/* language=JavaScript */ "it < other ? -1 : (it > other ? 1 : 0)") }

public actual object BigIntegers {
  // we don’t store the values because we trust the runtime environment to apply interning if it deems it useful
  public actual val ZERO: BigInteger get() = nativeBigInt(0)
  public actual val ONE: BigInteger get() = nativeBigInt(1)
  public actual val TWO: BigInteger get() = nativeBigInt(2)
}
