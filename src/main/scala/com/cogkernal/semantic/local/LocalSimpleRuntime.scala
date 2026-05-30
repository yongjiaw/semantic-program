package com.cogkernal.semantic
package local

import com.cogkernal.semantic.SaferContext.{Categorization, Dimension, MemoryPath}

class LocalSimpleRuntime[K, V] extends SaferContext with LocalComputationContext with LocalSimplePersistenceContext {
  override type Key = K
  override type Repr = V
  case class SimpleMemory(persisted: Persisted[K, V]) extends Memorized

  override type Memory = SimpleMemory

}
