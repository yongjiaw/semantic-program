package com.cogkernal.semantic.local

import com.cogkernal.semantic.ComputationContext
import com.cogkernal.semantic.PersistenceContext.*

trait LocalComputationContext extends ComputationContext {
  override type KC = LocalKeyedComputation
  
  override def join[K, V](left: LocalKeyedComputation[K, V], right: LocalKeyedComputation[K, V]): LocalKeyedComputation[(K, K), (V, V)] = {
    left.flatMap {
      (leftK, leftV) => right.data.map {
        (rightK, rightV) => (leftK, rightK) -> (leftV, rightV)
      }
    }
  }

  class LocalKeyedComputation[K, V](val data: Iterable[(K, V)]) extends KeyedComputation[K, V] {

    override def flatMap[K2, V2](f: (K, V) => Iterable[(K2, V2)]): LocalKeyedComputation[K2, V2] = LocalKeyedComputation(
      data.flatMap((k, v) => f(k, v))
    )

    override def split[S](f: (K, V) => S): Map[S, LocalKeyedComputation[K, V]] = {
      data.map((k, v) => f(k, v) -> (k, v)).groupBy(_._1).map {
        case (role, elements) => role -> LocalKeyedComputation(elements.map(_._2))
      }
    }
    
    // keyed computation only declare the key, the uniqueness access is not guaranteed
    lazy val map: Map[K, V] = {
      data.groupBy(_._1).map {
        case (key, values) =>
          val uniqueValues = values.map(_._2).toSet.size
          if (uniqueValues > 1) throw new Exception(s"duplicated values for ${key}: ${values.size}, $values")
          key -> values.head._2
      }
    }

    // after aggregation the data should be guaranteed to be unique for persistence and keyed retrieval
    // but it's still the persistence layers responsibility to validate
    // TODO in streaming mode, the persistence layer should be synced using the same merging function
    override def reduce(reducer: (V, V) => V): LocalKeyedComputation[K, V] = {
      LocalKeyedComputation(
        data.groupBy(_._1).mapValues {
          values =>
            values.map(_._2).reduce((a, b) => reducer(a, b))
        }.toMap
      )

    }

    override def union(other: LocalKeyedComputation[K, V]): LocalKeyedComputation[K, V] = LocalKeyedComputation(data ++ other.data)
  }
}
