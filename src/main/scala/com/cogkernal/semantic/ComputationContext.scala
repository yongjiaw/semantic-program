package com.cogkernal.semantic

/**
 * Any dataflow system can support this interface (Spark, Flink, Beam)
 */
trait ComputationContext {
  type KC[K, V] <: KeyedComputation[K, V]

  def join[K, V](left: KC[K, V], right: KC[K, V]): KC[(K, K), (V, V)]

  trait KeyedComputation[K, V] {

    def flatMap[K2, V2](f: (K, V) => Iterable[(K2, V2)]): KC[K2, V2]

    // map is value only, preserving key
    def map[V2](f: V => V2): KC[K, V2] = flatMap((k, v) => Iterable.single(k -> f(v)))

    // low cardinality splitting/grouping used for decomposition/factor and categorization/refine
    def split[S](f: (K, V) => S): Map[S, KC[K, V]]

    def reduce(reducer: (V, V) => V): KC[K, V]

    // union implementation can choose to mutate the first one for streaming update
    def union(other: KC[K, V]): KC[K, V]

  }
  
}
