# Semantic Program

Semantic Program is a framework for building compositional memory and evolving semantic state for autonomous systems.

The central thesis is:

> A small algebra of semantic state transitions can turn streams of experience into compositional, queryable, and scalable memory.

Instead of treating long-term memory as a passive store of past instances, Semantic Program models memory as evolving semantic state: structured, anchored, transformed, queried, compressed, and extended through reusable computation.

The framework connects two traditions:

- **Agent memory and cognitive architectures:** semantic memory, episodic memory, production systems, query-driven reasoning, and agents that accumulate observations and evolve structured knowledge over time.

- **Distributed computation and storage systems:** scalable, generic containers for computation, retrieval, and persistence, including dataflow, stream/batch processing, key-value stores, and query engines.

## Motivation

Modern autonomous systems increasingly need memory that is more structured than raw context windows, logs, databases, or vector retrieval alone.

A scientific discovery agent, for example, should not merely retrieve past experiments. It should be able to ingest observations, anchor them to stable semantic dimensions, decompose observations into reusable parts, compose new candidate states and hypotheses, refine memory into meaningful categories, and query, persist, compress, and replay learned structure.

Semantic Program explores this problem through a compositional architecture for memory, computation, and query.

## Scope Clarification: Two Related Problems

Semantic Program is motivated by two related but different problems. They share principles — semantic memory, agent control, structured learning, and compositional state evolution — but they have different runtimes and engineering constraints.

### 1. Semantic Control for Data Engineering Systems

The first problem is an industry/data-engineering problem:

> How can a data pipeline safely evolve based on semantic-level knowledge, policies, constraints, lineage, and feedback, with increasing autonomy and minimal human intervention?

In this setting, a Soar-like agent can serve as the control and semantic reasoning layer. It reasons about schemas, dependencies, policies, ownership, lineage, quality, operational constraints, and safe evolution. The actual data pipeline runtime is external: Spark, Flink, Beam, Kafka, databases, orchestration systems, cloud data platforms, or other execution environments.

This problem has significant practical value because current industry systems still rely heavily on manual intervention for semantic data evolution, migration, validation, governance, and pipeline adaptation.

### 2. Extending Soar Semantic Memory Learning

The second problem is specifically about Soar:

> How can Soar's built-in semantic memory support richer semantic learning while preserving Soar's architectural commitments?

In this setting, the runtime is Soar itself: production rules, working memory, Rete matching, semantic memory, and local architectural storage. This imposes different constraints, especially around real-time behavior, architectural generality, and keeping productions and working memory in control.

The goal is not to add hidden background reasoning. The architecture should provide generic, syntactic mechanisms. Productions and working memory should still determine when those mechanisms are used and what structures make them meaningful.

For example, dimension and identity may provide the precondition for generic architectural reconciliation: when productions create a contribution with a dimension and a deterministically computed identity, the architecture can know when merge, aggregation, or statistical learning outside working memory is well-defined.

### Relationship

These two problems are connected, but they should not be collapsed.

The broader SAFER framework applies to both: semantic dimensions, identity, anchoring, factoring, expansion, refinement, lineage, and controlled memory evolution.

However, the implementation commitments differ:

- In the data-engineering problem, SAFER can use external scalable runtimes and persistence systems.
- In the Soar problem, SAFER must fit within Soar's production/working-memory/semantic-memory architecture and respect its real-time and architectural constraints.

Keeping this distinction explicit helps keep discussions properly scoped.

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

## [SAFER](SAFER.md) Algebra

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

Soar provides a strong model of symbolic working memory, production rules, episodic memory, and semantic memory. Semantic Program explores how this can be expressed in a Soar-native way while also remaining useful as a broader framework for semantic state evolution.

For Soar specifically, many SAFER-style operations can be prototyped with existing `smem` mechanisms:

- Forward dependency navigation can be represented as retrieval.
- Backward navigation can use cue/activation-based retrieval when it is dimension-scoped.
- Categorization/refinement can be represented as ordinary symbolic attributes with reserved meanings.
- Current `smem` can be viewed roughly as the special case where the semantic store is one global dimension and each object has its own LTI identity.

The main additional commitment is making dimension and identity first-class so storage can trigger anchored reconciliation rather than creating unrelated LTIs. This connects to the idea that some learning can happen outside working memory while still keeping productions and working memory in control. The architecture would provide generic syntactic reconciliation or aggregation once productions have created the dimension, identity, and contribution structure that make the operation well-defined.

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

- [Soar Workshop 2026: A Dataflow Framework for Structured Memory and Learning in Soar](SoarWorkshop2026.pdf)

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
