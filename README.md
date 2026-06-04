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

In data applications, keyed updates are already common: user profiles, entity stores, feature stores, knowledge graphs, CDC pipelines, event-sourced applications, and stateful stream processors all maintain state by identity. Many systems also enforce local policies, domain rules, schemas, validation logic, lineage conventions, and aggregation behavior.

The harder problem is end-to-end automation across domains. In real systems, the semantic and operational meaning of an update is often distributed across backend conventions, schemas, pipeline code, orchestration rules, data contracts, catalogs, versioning systems, retention policies, and application logic:

```text
What dimension is being updated?
What identity system does the key belong to?
Is the value observed, inferred, predicted, derived, or corrected?
What source or trace produced it?
Which policy decides whether the update is automatic, reviewed, blocked, or rolled back?
Which schema version does it belong to?
How should rollback, replay, retention, and expiration work?
Which aggregation scope should it affect?
Which downstream compositions should it update, invalidate, or expand?
Which domain owns the decision, and how does it compose with other domains?
```

Semantic Program makes this update contract explicit so that control policies can operate across heterogeneous domains and runtimes. The goal is not to replace domain-specific logic or remove human judgment, but to make semantic and operational commitments governable and composable through a unified control plane. Human intervention is one possible policy outcome: some changes may be automatic, some may require approval, some may request review, and some may be blocked or rolled back.

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

SAFER is not merely a query model. It is an algebra of **semantic state evolution**. In data systems, relational algebra and SQL are excellent for querying already-formed relations. SQL also has `INSERT`, `UPDATE`, and `MERGE`, but the application-level semantics of identity resolution, provenance, source status, aggregation impact, category scope, and downstream semantic consequences are usually defined outside the query language.

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

In data applications, this contract keeps meaning and lineage from being split across schemas, storage backends, and custom application logic. With the contract in place, aggregation, retrieval, expansion, and explanation can be implemented consistently above different storage engines.

For instance-oriented memory designs, the same contract clarifies how stored experiences become accumulated semantic state. Instance memory can store many experiences; dimension, identity, and lineage specify how those experiences participate in aggregation, retrieval scope, and compositional reuse.

## Learning as Semantic Commitment

Learning requires semantic commitment: deciding what an observation is about, what identity it updates, what evidence it should be reconciled with, and what derived state should be trusted for future use. These decisions can be wrong or lossy, but they are also what make memory reusable.

The conservative choice is to defer interpretation until query time. This preserves evidence and flexibility, but it can repeatedly push the same work into retrieval, reconstruction, aggregation, policy evaluation, and downstream reasoning. Engineering systems often make earlier commitments under scalability pressure: materialized views, feature stores, indexes, caches, stateful stream processors, data warehouses, and microservices all maintain derived state before query time.

Neither choice is universally correct. The important design question is where semantic commitment should happen under a given set of constraints, and under which control policy. Human approval, review, escalation, blocking, rollback, and retention are part of that policy, not exceptions to it. Semantic Program makes these choices explicit by separating evidence, identity, lineage, update policy, aggregation, derived state, and operational semantics such as schema evolution, versioning, rollback, replay, and retention.


## Design Spectrum: Soar, Data Applications, and Hybrid Control

SAFER provides a shared vocabulary for systems that make different architectural commitments around memory, update, reasoning, and execution. The same algebra can be applied in several settings.

In **data applications**, the focus is end-to-end semantic automation over scalable state maintenance: keyed updates, aggregation, joins, lineage tracking, materialization, persistence, validation, schema evolution, version control, rollback, replay, retention, and downstream invalidation. Individual domains can already implement custom policies and domain logic. The harder challenge is coordinating those semantic and operational commitments across many domains through a unified control plane so that policy, identity, lineage, aggregation, lifecycle, human review, and downstream effects remain explicit and composable.

In **Soar-native semantic memory**, the focus is cognitive control. Soar places active reasoning in working memory and productions, with semantic memory, episodic memory, and external modules supporting that control loop. SAFER can clarify which semantic-memory capabilities are naturally expressed through Soar's native mechanisms and which additional memory-side options, such as dimension-aware anchoring, trace-aware aggregation, or Expand-style generation, may be worth making explicit.

A third setting is the **hybrid application**: Soar can serve as the control layer for a semantic data application. In this pattern, Soar decides goals, operators, attention, decomposition, categorization, policies, and when to commit, query, escalate, or request human review. A SAFER-style data runtime maintains the external semantic state: keyed updates, lineage, aggregation, persistence, lifecycle policy, and candidate generation over larger data collections. This separates autonomous control from scalable state maintenance.

```text
Data-application runtime:
  scalable keyed update, materialization, aggregation, lineage, persistence

Data-application control plane:
  cross-domain policy, ownership, validation, schema evolution, versioning,
  rollback, replay, retention, human review, commitment, invalidation, automation

Soar-native memory extension:
  production-centered control with explicit semantic-memory options

Soar-controlled semantic data application:
  Soar controls task reasoning and policy; SAFER runtime maintains governed semantic state

SAFER:
  shared vocabulary for dimension, identity, lineage, aggregation, expansion,
  lifecycle, and control
```

This spectrum is useful because it separates control choices from state-maintenance choices. For data applications, the main contribution is a unified semantic control plane for end-to-end automation across domains, including operational semantics such as schema evolution, versioning, rollback, replay, retention, human review, and downstream invalidation. For Soar, the same vocabulary clarifies which commitments should remain under production-centered control and which memory-side options may be useful. In hybrid systems, Soar can remain the control architecture while a SAFER data runtime provides disciplined semantic state evolution for applications that need scalable memory, aggregation, and persistence.

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

This is the data-application side of the spectrum. The framework can make stronger commitments because its purpose is governed state evolution, lineage, aggregation, and consistency across many custom keyed-update mechanisms.

## Relationship to Soar

Soar is an important reference point because it already has a mature and deliberate computational architecture: working memory, production rules, semantic memory, episodic memory, and external modules. Soar's design keeps domain reasoning and control in working memory and productions, and avoids placing implicit computation or reasoning inside long-term memory. That boundary is not an accident; it is one of the reasons Soar remains general and inspectable.

A Soar implementation would be a separate reference implementation of the pattern inside the Soar code base, using Soar's native control mechanisms. SAFER provides vocabulary for discussing which semantic-memory operations are represented through productions and external modules, and which operations may be useful as explicit memory-side capabilities.

For Soar, the design conversation can focus on choices such as:

```text
Production-centered expression:
  aggregation, reconstruction, and candidate generation are expressed through
  productions or external mechanisms.

Explicit memory-side support:
  semantic-memory commands make dimension and identity explicit;
  lineage is captured from working-memory traces;
  aggregation or Expand can be enabled under explicit scope.
```

A Soar application of the pattern could focus on:

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
| Factor | Productions perform domain-specific decomposition; memory may capture trace links among anchored elements. |
| Refine | Productions assign categories or labels; semantic memory may store them as retrieval and aggregation scopes. |
| Expand | Semantic memory may expose candidate generation over dimension-scoped memories. |
| Aggregation | Semantic memory may maintain statistical summaries over identity-linked, lineage-preserving traces. |

This framing keeps Soar's existing architecture central. SAFER provides names for the semantic-memory options around that architecture, including dimension-aware anchoring, trace-based aggregation, and Expand-style generation.

## Soar as Control for Semantic Data Applications

A separate but natural application is to use Soar as the control layer over a SAFER-style semantic data runtime. In this architecture, Soar does not need to turn semantic memory into a full data backend, and the data runtime does not need to own the agent's control logic.

The responsibilities can be separated as:

```text
Soar control layer:
  goals, operators, attention, decomposition, categorization, decisions to query or commit

SAFER semantic data runtime:
  dimension-scoped keyed update, lineage, aggregation, materialization, persistence, expansion
```

This is the implicit combination between cognitive architecture and data infrastructure. Soar supplies the active reasoning loop and control policy, including when to automate, when to ask for review, and when to block or roll back a commitment. SAFER supplies the disciplined state-evolution substrate for larger semantic data applications. The value is not that local domain logic becomes possible; it is that commitments across domains — including schema evolution, versioning, rollback, replay, retention, human intervention, and downstream invalidation — can be coordinated, audited, and automated through a shared control vocabulary.

## Aggregation and Expand

In a cognitive architecture such as Soar, semantic memory can often represent dimensions, identities, and nested categorizations as ordinary symbolic attributes. The design question is therefore not mainly representational; it is how much of the following behavior should remain expressed through productions and external modules, and how much should become explicit memory-side support.

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

In the Soar-style setting, semantic memory is commonly used through cue-based retrieval:

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

In a Soar implementation, productions or higher-level control decide whether a generated candidate is meaningful, valid, or worth anchoring. In a dataflow implementation, the same operation can be compiled into joins, products, or candidate-generation stages.

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

For data applications, the practical significance is end-to-end automation with explicit semantic control. Keyed update, aggregation, lineage, retrieval, policy, validation, schema evolution, versioning, rollback, replay, retention, human review, and downstream effects can be described using one shared vocabulary instead of being scattered across backend conventions and application-specific code. For Soar-style cognitive architectures, the same vocabulary clarifies which capabilities remain naturally expressed through working memory, productions, episodic memory, semantic memory, or external modules, and which additional memory-side options are worth making explicit.

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

Soar may become a separate reference implementation of the same pattern inside the Soar code base, using Soar's native control and memory mechanisms. A complementary path is a hybrid application in which Soar provides the control layer while a SAFER-style runtime maintains external semantic state for data-intensive memory, aggregation, and persistence.

## Talks

- [Soar Workshop 2026: A Dataflow Framework for Structured Memory and Learning in Soar](SoarWorkshop2026.pdf)

## Repository Direction

The project is moving toward a layered architecture:

```text
Control / Agent Layer
  - task policy
  - automation policy
  - human review and escalation policy
  - exploration strategy
  - optional Soar-based control over semantic data applications

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
  - update and lifecycle policies

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

In data applications, SAFER gives keyed updates a shared semantic vocabulary and connects them to aggregation, refinement, expansion, compositional reuse, policy, lifecycle, human review, and downstream control. The main contribution is end-to-end semantic automation across domains through a unified control plane, not the replacement of local domain logic or human judgment. This includes operational semantics such as schema evolution, versioning, rollback, replay, retention, and invalidation. In Soar-native semantic memory, SAFER provides a way to discuss the same operations as explicit architectural choices around memory commands, retrieval, trace capture, aggregation, and generation. In hybrid applications, Soar can provide the control layer while a SAFER-style runtime maintains governed semantic state outside the cognitive core.
