# Collections

Collection types and sequence utilities for cases that the standard library and `androidx.collection` don’t cover.

- `BitSet`: a mutable set of non-negative integers, stored as one bit per integer up to the largest element. It grows as needed, but never gives memory back.
  Use it when the elements are dense within that range; for few elements spread over a large range, a hash set such as `androidx.collection.MutableIntSet` needs less memory.

- `interleave`: lazily takes one element from each input sequence in turn, skipping exhausted inputs. It supports finite and infinite sequences without starving later inputs.
