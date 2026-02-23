package software.consistent.model

import kotlin.js.JsExport

@JsExport
public interface MType {
    public infix fun union(other: MType): MType = Union(this, other)

    public infix fun intersect(other: MType): MType = Intersection(this, other)

    public fun normalize(): MType = this
}

public interface MValue : MType
