# Semantic Program

Semantic Program is a framework for defining structured memory, computation, and query in a unified, compositional way.

The core idea is to separate:
- **what** structure and computation are defined (program)
- from **how** they are executed (runtime)

This enables exploration of richer declarative learning mechanisms, including:
- aggregation and statistical learning over memory
- composition across modalities
- structured abstraction learning

---

## Overview

Semantic Programs make symbolic structure explicit and compositional, allowing it to be explored and eventually learned.

The framework is built around:

- **Semantic Modules**  
  Reusable components grounded in the environment that define local computation  
  (e.g., perception, aggregation, prediction)

- **Semantic Program**  
  A compositional specification of data, computation, and query

- **Dataflow Runtime**  
  Executes learning and query transparently, separating definition from execution

- **Dimensions**  
  Shared structure that enables consistent composition across modules

---

## Status

🚧 This is an initial conceptual release.

Current contents:
- Presentation materials

Planned additions:
- Reference implementation
- Example domains (e.g., blocks world)
- Integration with Soar and other agent architectures

---

## Talks

- **Soar Workshop 2026**  
  *A Dataflow Framework for Structured Memory and Learning in Soar*

---
