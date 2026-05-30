# Semantic Program

Semantic Program is a framework for building compositional memory and evolving semantic state for autonomous systems.

The central thesis is:

> A small algebra of semantic state transitions can turn streams of experience into compositional, queryable, scalable, and incrementally maintainable memory.

Instead of treating long-term memory as a passive store of past instances, Semantic Program models memory as evolving semantic state: structured, anchored, transformed, queried, compressed, and extended through reusable computation.

The framework connects two traditions:

- **Agent memory and cognitive architectures:** semantic memory, episodic memory, production systems, query-driven reasoning, and agents that accumulate observations and evolve structured knowledge over time.
- **Distributed computation and storage systems:** scalable, generic infrastructure for computation, retrieval, keyed update, lineage, aggregation, and persistence, including dataflow, stream/batch processing, key-value stores, and query engines.

## Motivation

Modern autonomous systems increasingly need memory that is more structured than raw context windows, logs, databases, or vector retrieval alone.

A scientific discovery agent, for example, should not merely retrieve past experiments. It should be able to ingest observations, anchor them to stable semantic dimensions, decompose observations into reusable parts, compose new candidate states and hypotheses, refine memory into meaningful categories, and query, persist, compress, and replay learned structure.

Semantic Program explores this problem through a compositional architecture for memory, computation, update, and query.

## Core Idea

Semantic Program organizes memory around **semantic dimensions**.

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

The minimal framework commitment is:

```text
dimension + identity + lineage + keyed update
```

This contract is intentionally small. It says that durable semantic updates should be dimension-scoped, identity-keyed, lineage-preserving, and incrementally maintainable.

Many systems already perform keyed updates: user profiles, entity stores, feature stores, knowledge graphs, semantic memory systems, CDC pipelines, event-sourced applications, and stateful stream processors. The problem is that the semantic meaning of the update is usually buried in custom backend conventions or application code:

```text
What dimension is being updated?
What identity system does the key belong to?
Is the value observed, inferred, predicted, derived, or corrected?
What source or trace produced it?
Which aggregation scope should it affect?
Which downstream compositions should it update, invalidate, or expand?
```

Semantic Program makes this hidden contract explicit.

## [SAFER](SAFER.md) Algebra

The current formulation is organized around five semantic state transition operators:

| Operator | Purpose | Example |
|---|---|---|
| Source | Introduce evidence, observations, or external computation | Observed stack episodes become Compute(Stack) |
| Anchor | Commit evidence into dimension-scoped, identity-keyed semantic state | Merge and persist stack observations as Memory(Stack) |
| Factor | Decompose one dimension into component dimensions while preserving lineage | Stack -> top Block, bottom Block |
| Expand | Generate candidate compositions over keyed dimensions | Block + Block -> imagined Stack |
| Refine | Split memory into exclusive categories or retrieval scopes | Stack -> Stable / Unstable, Observed / Imagined |

SAFER is intended to be small enough to reason about, but expressive enough to describe how structured memory evolves over time.

A useful interpretation is:

```text
Source  - where evidence originates
Anchor  - how evidence becomes keyed semantic state
Factor  - how structure is decomposed while preserving lineage
Expand  - how stored dimensions generate candidate compositions
Refine  - how memory is partitioned into explicit scopes and categories
```

SAFER is not merely a query model. It is an algebra of **semantic state evolution**. Relational algebra and SQL are excellent for querying already-formed relations, but they do not provide a first-class semantic model of keyed update. SQL has `INSERT`, `UPDATE`, and `MERGE`, but identity resolution, provenance, source status, aggregation impact, category scope, and downstream semantic consequences remain outside the language.

## Anchor as Keyed Semantic Update

Anchor is the operation that turns evidence into accumulated semantic state.

An Anchor operation should make explicit:

```text
dimension:    what kind of semantic state is being updated?
identity:     which durable semantic object is this about?
value:        what observation, structure, category, or derivation is being committed?
lineage:      where did this contribution come from?
status:       observed, inferred, predicted, derived, corrected, hypothetical?
policy:       replace, merge, accumulate, decay, version, aggregate, or reconcile?
```

Without this contract, meaning and lineage are scattered across schemas, storage backends, and custom application logic. With this contract, aggregation, retrieval, expansion, and explanation can be implemented generically above different storage engines.

This is one of the main differences between Semantic Program and instance-based memory. Instance memory can store many experiences, but without dimension, identity, and lineage, it does not naturally support principled semantic aggregation or compositional reuse.

## Dataflow Interpretation

Semantic operators do not replace dataflow systems. They sit above them.

A Semantic Program describes what a memory transition means. A dataflow runtime provides how it executes.

| Semantic operator | Dataflow interpretation |
|---|---|
| Source | Create keyed computation from external input |
| Anchor | Union, reduce, enrich, reconcile, and persist keyed semantic state |
| Factor | Map or decompose from one dimension to another while preserving lineage |
| Expand | Join, product, candidate generation, and composition across dimensions |
| Refine | Split, categorize, or partition into exclusive memory scopes |

In a dataflow framework, SAFER can be implemented directly as a runtime discipline:

```text
all updates declare dimension and identity
all transformations preserve lineage
all materializations are keyed semantic updates
aggregation is incrementally maintained
refinement creates explicit category scopes
expand generates candidate compositions
```

This version can be opinionated because data applications already need governed state evolution, lineage, aggregation, and consistency.

## Relationship to Soar

Soar is an important reference point because it already has a mature and deliberate computational architecture: working memory, production rules, semantic memory, episodic memory, and external modules. Soar's design keeps domain reasoning and control in working memory and productions, and avoids placing implicit computation or reasoning inside long-term memory. That conservative boundary is a strength of the architecture.

SAFER should therefore be understood as a distinct application pattern for Soar, not a default runtime target or a subordinate binding. A Soar implementation should live in the Soar code base and use Soar's native control mechanisms.

The role of SAFER is to make the architectural tradeoff explicit. If semantic memory avoids aggregation, candidate generation, or other implicit computation, the system preserves a clean and general control model. The tradeoff is that some forms of learned semantic abstraction must be implemented manually through productions or external mechanisms. SAFER provides a vocabulary for evaluating those options deliberately rather than treating the most conservative option as the default.

A Soar implementation of the pattern could focus on:

```text
1. explicit dimension and identity in semantic-memory commands
2. automatic working-memory trace capture among anchored structures
3. optional scope-aware aggregation and associative learning
4. retrieval augmented by learned associations when enabled
5. Expand-style candidate generation over stored dimensions
```

In this setting, several SAFER concepts map naturally onto Soar:

| SAFER concept | Soar interpretation |
|---|---|
| Source | Working memory is already the source. |
| Anchor | A semantic-memory command saves selected WM structures under explicit dimension and identity. |
| Factor | Productions perform domain-specific decomposition; memory captures trace links among anchored elements. |
| Refine | Productions assign categories or labels; semantic memory stores them as retrieval and aggregation scopes. |
| Expand | Semantic memory may expose candidate generation over dimension-scoped memories. |
| Aggregation | Semantic memory may maintain statistical summaries over identity-linked, lineage-preserving traces. |

This framing respects Soar's generality. Soar can already represent rich symbolic structures and connect to non-symbolic modules. SAFER clarifies a possible memory-side extension point: whether dimension-scoped aggregation and candidate generation should remain external/manual, or become explicit semantic-memory capabilities under Soar's existing control architecture.

## Aggregation and Expand

Current semantic memory systems can often simulate dimensions, identities, and nested categorizations as ordinary symbolic attributes. The representational problem is usually not the central issue.

The main design question is operational:

```text
1. scope-aware aggregation
2. generative expansion
```

### Scope-aware aggregation

If observations are anchored with dimension, identity, category, and lineage, semantic memory can learn statistical associations over the resulting traces.

For example:

```text
Block b1: Color = red,  Density = high
Block b2: Color = red,  Density = high
Block b3: Color = blue, Density = low
```

The memory system can learn:

```text
Within Block:
  Color = red predicts Density = high
```

The trace defines the candidate evidence structure. Statistical learning decides which associations are promoted. The system should not mechanically return every co-traced property; it should promote only statistically supported associations, with support, confidence, and provenance.

Aggregation is mostly a hidden semantic-memory effect of making dimension, identity, categorization, and lineage explicit.

### Expand as candidate generation

Traditional semantic memory is mostly cue-based retrieval:

```text
Given a cue, retrieve matching memory.
```

Expand adds a complementary operation:

```text
Given dimensions, roles, scopes, and constraints,
generate candidate combinations.
```

For example:

```text
Generate candidate block pairs
within the stacking-task scope
where the bottom block is likely denser than the top block.
```

Productions or higher-level control still decide whether a generated candidate is meaningful, valid, or worth anchoring. But memory can provide the candidate space.

## Known Functions and Semantic Reconstruction

Aggregation learns associations, but some reconstruction requires known functional relationships.

For example:

```text
Weight = Size × Density
```

The goal is not for semantic memory to learn arbitrary functions. A practical design is to let semantic memory store **semantic function schemas**, while productions, RHS functions, or external modules perform the actual computation.

A function schema records:

```text
Function: block-weight-relation
Scope:    Block
Variables: Size, Density, Weight
Relation:  Weight = Size × Density

Supported derivations:
  Size + Density    -> Weight
  Weight + Size     -> Density
  Weight + Density  -> Size
```

Semantic memory stores the signature and applicability. Productions match the schema, bind available inputs, invoke the computation through normal mechanisms, and optionally anchor the derived result back into semantic memory with provenance.

Derived values should be marked differently from observed values:

```text
Weight = heavy
  status = derived
  derived-by = block-weight-relation
  inputs = Size, Density
```

This supports reconstruction, explanation, and residual/error learning while preserving the architectural boundary between memory representation and active control.

## Predictive Compression

The broader framework can be interpreted as a form of predictive compression:

```text
observations + lineage -> learned summaries + residuals
partial cue + learned summaries + functions -> reconstructed or augmented candidate state
```

The conservative starting point is identity-scoped statistical aggregation. Repeated trace patterns are compressed into learned associations with support and confidence, while exceptions remain available through episodic traces or residual evidence.

Arbitrary dimension-level predictive models may be useful in broader applications, but they should be treated as extension points rather than core SAFER primitives.

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
  -> Density

Stack
  -> Stable / Unstable
  -> Observed / Imagined
```

The domain runtime decides how to decompose and categorize the situation. Anchor commits selected structures under explicit dimensions and identities, while lineage records how the pieces are connected.

Over many examples, memory can learn:

```text
Within Block:
  Color = red predicts Density = high
```

If a known function schema declares:

```text
Weight = Size × Density
```

then retrieval or expansion can combine:

```text
known:        Size = large
learned:      Color = red -> Density = high
function:     Size × Density -> Weight
reconstruct:  Weight = heavy
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

The practical significance is not that systems have never performed keyed update, aggregation, or retrieval. They have. The significance is that these operations are usually scattered across backend conventions and application-specific code. SAFER gives them a shared semantic vocabulary and makes the consequences — aggregation, refinement, expansion, and compositional reuse — explicit.

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
- additional examples for scientific discovery and AI memory systems

Soar may become a separate reference implementation of the same pattern inside the Soar code base, using Soar's native control and memory mechanisms and making any additional semantic-memory commitments explicit.

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
  - aggregation
  - expansion
  - semantic function schemas

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

## Summary

SAFER’s core contribution is the explicit modeling of semantic state evolution.

The minimal framework contract is simple:

```text
dimension + identity + lineage + keyed update
```

This contract enables stronger operations:

```text
aggregation over explicit semantic scopes
retrieval augmented by learned associations
candidate generation over stored dimensions
function-aware reconstruction through declared semantic schemas
incremental maintenance of compositional semantic state
```

This explains why SAFER is useful even though many systems already perform keyed updates informally. SAFER gives those updates a shared semantic vocabulary and makes the consequences — aggregation, refinement, expansion, and compositional reuse — explicit.
