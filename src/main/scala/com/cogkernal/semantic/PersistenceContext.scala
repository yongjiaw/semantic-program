package com.cogkernal.semantic

import PersistenceContext.*

trait PersistenceContext {
  Self: ComputationContext =>

  type PersistSpec
  type Persisted[K, V] <: PersistedBase[K, V]

  trait PersistedBase[K, V] {
    def retrieval: Retrieval[K, V]

    // after materialization, the dataPlan should support replay based on materialization spec
    // the upstream computation may not be directly responsible any more
    def dataPlan: KC[K, V]

    def sample(rate: Double = 1, limit: Long = Long.MaxValue): Iterable[(K, V)]

    def foreach(update: V => Unit): Unit

  }

  def persist[K, V](
                     spec: PersistSpec,
                     plan: KC[K, V]
                   ): Persisted[K, V]
}

object PersistenceContext {
  // in distributed mode, the getter should be serialized to workers with all the read access information
  trait Retrieval[K, V] extends Serializable {
    def retrieve(key: K): Option[V]
  }
}
