package software.consistent.collections

internal actual typealias BitStorage = java.util.BitSet

internal actual fun BitStorage.copy(): BitStorage = clone() as BitStorage
