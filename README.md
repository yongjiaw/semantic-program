# Semantic Program

Semantic Program is a framework for building compositional memory and semantic state systems for AI agents.

The central thesis is:

> A small algebra of semantic state transitions can turn streams of experience into compositional, queryable, and scalable memory.

Instead of treating long-term memory as a passive store of past instances, Semantic Program models memory as evolving semantic state: structured, anchored, transformed, queried, compressed, and extended through reusable computation.

The framework connects three traditions:

- Cognitive architectures and agent memory: symbolic memory, semantic memory, episodic memory, production systems, and query-driven reasoning.
- Dataflow and distributed systems: map, reduce, join, split, union, persistence, retrieval, and scalable execution.
- AI systems and scientific discovery: systems that accumulate observations, form structured abstractions, test hypotheses, and evolve memory over time.

## Motivation

Modern AI systems increasingly need memory that is more structured than raw context windows, logs, databases, or vector retrieval alone.

A scientific discovery agent, for example, should not merely retrieve past experiments. It should be able to ingest observations, anchor them to stable semantic dimensions, decompose observations into reusable parts, compose new candidate states and hypotheses, refine memory into meaningful categories, and query, persist, compress, and replay learned structure.

Semantic Program explores this problem through a compositional architecture for memory, computation, and query.

## Core Idea

Semantic Program organizes memory around semantic dimensions.

Examples include:

- Stack
- Block
- Color
- Size
- Weight
- Stability
- Experiment
- Material
- Measurement
- Hypothesis

Each dimension owns or participates in structured state. State evolves through explicit semantic transitions rather than through unstructured accumulation alone.

This makes memory compositional, queryable, inspectable, scalable, and compatible with AI agents. Symbolic handles can coexist with learned representations, statistical models, and dataflow computation.

## SAFER Algebra

The current formulation is organized around five core semantic state transition operators:

| Operator | Purpose | Example |
|---|---|---|
| Source | Introduce external observations as computation | Observed stack episodes become Compute(Stack) |
| Anchor | Consolidate computation into dimension-anchored memory | Merge and persist stack observations as Memory(Stack) |
| Factor | Decompose one dimension into component dimensions | Stack -> top Block, bottom Block |
| Expand | Compose across dimensions to form new candidate state | Block + Block -> imagined Stack |
| Refine | Split memory into exclusive categories | Stack -> Stable / Unstable, Observed / Imagined |

SAFER is intended to be small enough to reason about, but expressive enough to describe how structured memory evolves over time.

## Dataflow Bridge

Semantic operators do not replace dataflow systems. They sit above them.

A Semantic Program describes what a memory transition means. A dataflow runtime provides how it executes.

Typical execution primitives include:

- map
- reduce
- join
- split
- union
- persist
- retrieve

The semantic operators can be interpreted through these lower-level operations:

| Semantic operator | Dataflow interpretation |
|---|---|
| Source | Create keyed computation from external input |
| Anchor | Union, reduce, enrich, and persist |
| Factor | Map or decompose from one dimension to another |
| Expand | Join and compose across dimensions |
| Refine | Split or categorize into exclusive memory partitions |

This allows the same semantic formulation to be implemented over local execution, key-value stores, Spark, Flink, Beam, or other distributed runtimes.

## Relationship to Soar

The initial motivating context is integration with the Soar cognitive architecture.

Soar provides a strong model of symbolic working memory, production rules, episodic memory, and semantic memory. Semantic Program explores how this can be extended with a dataflow-based layer for structured memory evolution.

The goal is not to replace production rules or symbolic reasoning. The goal is to give agent memory richer computational support:

- learned regularities over experience
- structured abstractions over semantic dimensions
- compositional retrieval beyond instance matching
- explicit lineage from observations to derived memory
- symbolic handles grounded in dataflow and learned representations

The first prototype examples use a blocks-world-style domain involving stacks, blocks, colors, sizes, weight, and stability.

## Example: Blocks and Stack Stability

A simple example demonstrates the idea.

Observed stack episodes can be decomposed into blocks. Blocks can be decomposed into properties such as color and size. Weight can be modeled from density and size. Stack stability can be modeled from relational structure, such as:

```text
Weight(top) < Weight(bottom)
```

From observed stacks, the system can induce structured memory:

```text
Stack
  -> top Block
  -> bottom Block

Block
  -> Color
  -> Size
  -> Weight

Stack
  -> Stable / Unstable
  -> Observed / Imagined
```

This example is intentionally simple. The purpose is to show how memory can evolve from experienced instances into reusable semantic state.

## Why This Matters

Many AI systems today rely on combinations of prompt context, vector retrieval, logs, databases, tools, and model parameters.

Semantic Program explores a more explicit architecture where memory evolution itself is programmable and inspectable.

This is especially relevant for systems that need to accumulate and reason over structured experience, such as:

- AI agents
- scientific discovery systems
- autonomous experimentation systems
- personal or organizational memory systems
- data and ML platforms with evolving semantic models
- hybrid symbolic-statistical systems

## Current Status

This repository is an early research workspace.

Current contents include:

- workshop presentation materials
- evolving design notes
- prototype formulations of semantic state and SAFER operators

Planned additions include:

- reference implementation
- blocks-world example domain
- local dataflow runtime
- persistence abstraction
- Soar integration examples
- additional examples for scientific discovery and AI memory systems

## Talks

- Soar Workshop 2026: A Dataflow Framework for Structured Memory and Learning in Soar

## Repository Direction

The project is moving toward a layered architecture:

```text
Control / Agent Layer
  - task policy
  - automation
  - exploration strategy

Semantic Layer
  - dimensions
  - categories
  - identities
  - representations
  - SAFER operators
  - lineage

Bridging Layer
  - compilation from semantic operators to dataflow programs
  - runtime binding
  - persistence strategy

Generic Infrastructure
  - local runtime
  - key-value stores
  - Spark / Flink / Beam style execution
  - long-term storage
```

The long-term goal is a framework where memory, computation, query, persistence, compression, and learning can be defined compositionally.

## Author

Yongjia Wang

GitHub: https://github.com/yongjiaw
