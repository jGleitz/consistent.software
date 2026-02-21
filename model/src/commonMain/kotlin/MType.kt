package software.consistent.model

interface MType {
    infix fun union(other: MType): MType = Union(this, other)
    infix fun intersect(other: MType): MType = Intersection(this, other)

    fun normalize(): MType = this
}

interface MValue: MType


