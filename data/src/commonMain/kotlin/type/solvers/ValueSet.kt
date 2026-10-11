package software.consistent.data.type.solvers

import software.consistent.data.type.solvers.NormalizationComparable as GenericNormalizationComparable


internal interface ValueSet<TValue> {
  operator fun contains(value: TValue): Boolean

  interface NormalizationComparable<TValue, Self : ValueSet.NormalizationComparable<TValue, Self>> : ValueSet<TValue>,
    GenericNormalizationComparable<Self>
}
