package com.cogkernal.semantic
package local

trait LocalSimplePersistenceContext extends PersistenceContext with LocalComputationContext {
  override type PersistSpec = Any
  override type Persisted[K, V] = PersistedBase[K, V]

  override def persist[K, V](spec: Any, plan: KC[K, V]): Persisted[K, V] = {
    new PersistedBase[K, V] {
      // in a real dataflow implementation, this plan should be based on the materialized data
      // instead of copying the upstream computation
      override val dataPlan: KC[K, V] = plan

      // TODO, sample is a debug interface, it may needs to be removed entirely and only exposed in MaterializationContext
      override def sample(rate: Double = 1, limit: Long = Long.MaxValue): Iterable[(K, V)] = plan.data

      override def foreach(update: V => Unit): Unit = plan.map.values.foreach(update)

      override val retrieval: PersistenceContext.Retrieval[K, V] = {
        new PersistenceContext.Retrieval[K, V] {
          override def retrieve(key: K): Option[V] = dataPlan.map.get(key)
        }
      }
    }
  }
}

object LocalSimplePersistenceContext extends LocalSimplePersistenceContext