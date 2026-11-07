# Collections

Collections specialised for primitive values, for cases that the standard library and `androidx.collection` don’t cover efficiently.

- `BitSet`: a mutable set of non-negative integers, stored as one bit per integer up to the largest element. It grows as needed, but never gives memory back.
  Use it when the elements are dense within that range; for few elements spread over a large range, a hash set such as `androidx.collection.MutableIntSet` needs less memory.
