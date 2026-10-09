package software.consistent.model.type.solvers

import software.consistent.model.type.solvers.NormalizationComparable as GenericNormalizationComparable


internal interface ValueSet<TValue> {
  operator fun contains(value: TValue): Boolean

  interface NormalizationComparable<TValue, Self : ValueSet.NormalizationComparable<TValue, Self>> : ValueSet<TValue>,
    GenericNormalizationComparable<Self>
}

internal inline operator fun <reified TValue> ValueSet<TValue>.contains(value: Any) = value is TValue && value in this
