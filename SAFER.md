# SAFER: A Formal Algebra of Semantic State Evolution

## 1. Purpose

SAFER is a typed algebra for semantic state evolution.

It defines how external experience, derived computation, anchored memory, decomposition, composition, and refinement interact in a compositional memory system.

The central claim is:

> SAFER is to evolving semantic memory what relational algebra is to relations: a small compositional core that separates logical semantics from domain rules, persistence strategy, and physical execution.

SAFER is more complex than relational algebra because it is not only transforming extensional data. It is transforming memory state with:

- semantic dimensions
- keyed computation
- anchored memory
- lineage
- reconciliation
- categorization
- persistence specifications
- memory wrappers
- optional compression, prediction, retention, streaming, snapshotting, and rollback semantics

The purpose of this note is to separate the **core algebra** from the **plugin actions**, **persistence/memory specifications**, and **dataflow substrate**.

---

## 2. Layered View

SAFER should be understood as a layered algebraic system.

```text
SAFER semantic algebra
  - Source
  - Anchor
  - Factor
  - Expand
  - Refine

Plugin action rules
  - connect
  - merge
  - enrich
  - decompose
  - compose
  - categorize
  - persistSpec
  - memorize

Persistence and memory specifications
  - eager vs lazy
  - streaming vs snapshot
  - shared persistence vs new persistence
  - cache lifetime
  - retention
  - rollback
  - compression
  - prediction/model training
  - custom memory wrapper semantics

Dataflow substrate algebra
  - map
  - flatMap
  - split
  - reduce
  - union
  - join
  - persist
  - retrieve
```

The SAFER kernel does not decide every storage or runtime tradeoff. It defines the semantic transition shape. The plugin actions, persistence specifications, memory specifications, and runtime contexts decide how those transitions are materialized.

This is common. One algebra is often layered on top of another:

```text
SQL over relational algebra
Relational algebra over set theory
Dataframe APIs over relational/dataflow plans
Linear algebra over scalar fields
Tensor algebra over array operations
Functional APIs over lambda calculus or graph reduction
SAFER over keyed dataflow and persistence algebra
```

---

## 3. Analogy with Relational Algebra

Relational algebra has primitive operators such as:

```text
select
project
join
union
difference
rename
```

These operators do not know the business domain. Domain meaning enters through predicates, expressions, schemas, join conditions, constraints, and aggregate functions.

For example:

```sql
SELECT *
FROM customers
WHERE lifetime_value > 10000
```

The algebraic operator is:

```text
select
```

The domain-specific predicate is:

```text
lifetime_value > 10000
```

The physical runtime may use:

```text
scan
index lookup
filter pushdown
distributed execution
vectorized execution
```

SAFER follows the same separation.

For example:

```text
Refine(Memory[Customer], highValueRule)
```

The algebraic operator is:

```text
Refine
```

The domain-specific rule is:

```text
highValueRule(customer) = customer.lifetimeValue > 10000
```

The runtime may implement it through:

```text
split
filter
partition
persist
```

The pattern is:

```text
Algebraic operator = generic semantic transition form
Plugin rule        = local semantic meaning
Runtime substrate  = physical execution strategy
```

---

## 4. Core Objects

SAFER is a multi-sorted algebra. The main sorts are:

```text
Dimension
Categorization
Category
Lineage
ComputeWithLineage
MemoryWithLineage
MemoryPath
MemoryVersionPath
```

### 4.1 Dimension

A `Dimension` is a semantic type.

Examples:

```text
Stack
Block
Color
Size
Weight
Stability
Experiment
Material
Measurement
Hypothesis
```

A dimension identifies the semantic space in which a memory or computation lives.

```text
Dimension(name)
```

### 4.2 Categorization and Category

A `Categorization` names an exclusive partitioning scheme over a memory path.

Examples:

```text
Stability
Reality
ColorClass
WeightClass
Outcome
RecencyWindow
SamplingBucket
ConfidenceBand
OperationalSubset
```

A `Category` is a named subset within a categorization.

Examples:

```text
Stable / Unstable
Observed / Imagined
Red / Blue
Success / Failure
MostRecent100 / Older
HighConfidence / LowConfidence
```

A category is scoped by its categorization:

```text
Category(name, categorization)
```

A category is not necessarily an ontological class. It may represent a semantic class, a time window, a sampling strategy, a confidence band, or an optimization-specific subset.

This is important:

> Categorization is a generalized filtering operator. Each categorization is exclusive and single-valued. Multiple categorizations, not multi-label categories, represent overlapping views.

### 4.3 MemoryPath

A `MemoryPath` is a semantic address for memory.

```text
MemoryPath = Dimension + Seq[Category]
```

The root memory of a dimension is:

```text
MemoryPath.root(Dimension)
```

A refined memory path appends categories:

```text
MemoryPath(Dimension("Stack"), Seq(Category("Stable", Stability)))
```

A memory path is the logical address of a category-refined memory.

### 4.4 MemoryVersionPath

A `MemoryVersionPath` is a versioned memory address.

```text
MemoryVersionPath = AnchorLineage + Seq[CategoryVersion]
```

It records not only the dimension and category path, but also the anchor lineage and category rule versions.

This is important for cache addressing, materialization identity, rollback, lineage-aware query, and debugging.

---

## 5. Compute and Memory

SAFER distinguishes ephemeral computation from anchored memory.

### 5.1 Compute

A compute object is:

```text
ComputeWithLineage = KC[Key, Repr] + Lineage.Compute
```

It is not an unstructured bag. It is a **lineage-bearing keyed computation plan**.

The key point:

> `KC[Key, Repr]` is logically keyed by definition, but physical uniqueness does not need to be eagerly enforced at every intermediate point.

A `KC` may be a future-like, lazy, streaming, or distributed computation. It carries key structure, but materialization and reconciliation happen at explicit boundaries.

If multiple contributions for the same key arise during `Source`, `Factor`, `Expand`, or alignment, they are reconciled at `Anchor` through the injected `merge` rule before becoming memory.

So the correct statement is:

```text
Compute[D] is logically keyed and lineage-indexed.
Anchor is the semantic consolidation boundary that materializes aligned keyed contributions into memory.
```

### 5.2 Memory

A memory object is:

```text
MemoryWithLineage = Memory + Lineage.Memorize + optional parent
```

where:

```text
Memory <: Memorized
Memorized contains Persisted[Key, Repr]
```

So a memory is not defined in the core algebra as only an immutable snapshot, mutable store, or versioned map. Those are persistence/memory-spec choices.

At the algebra level:

```text
Memory[D] = memorized persisted keyed state with lineage
```

The persistence layer may implement:

```text
streaming append
snapshot materialization
shared persistence
new persistence
retention
rollback
compression
model training
custom retrieval
```

The core SAFER algebra only requires that memory is addressable by dimension/path and key, and that it carries lineage.

---

## 6. Lineage

Lineage is a first-class part of SAFER.

There are two main lineage families:

```text
Lineage.Compute
Lineage.Memorize
```

Compute lineages include:

```text
Source
Factor
Expand
```

Memorize lineages include:

```text
Anchor
Refine
```

This distinction is central:

```text
Source, Factor, Expand produce computation.
Anchor and Refine produce memorized state.
```

However, `Anchor` and `Refine` do not have the same role.

```text
Anchor = reconciliation and consolidation
Refine = categorized memory materialization/cache
```

A memorized lineage can be viewed as a producer through:

```text
asProducer
```

This allows existing memory to be reused as a computation source. The exact materialization behavior is determined by the persistence and memory specifications.

### 6.1 What Lineage Does Not Need to Encode

Lineage does not need extra ad hoc annotations such as:

```text
observed
derived
imagined
predicted
confirmed
rejected
```

The core semantic vocabulary is:

```text
Dimension
Categorization
Category.Name
```

Everything else about how a state was produced is factored into the program/action reference:

```text
Lineage.Source(..., action.reference, ...)
Lineage.Anchor(..., action.reference)
Lineage.Factor(..., action.reference)
Lineage.Expand(..., action.reference)
Lineage.Refine(..., action.reference)
```

So observed, imagined, predicted, and derived states should be represented as dimensions, categorizations/categories, or meanings inside the referenced program/action spec, not as separate built-in lineage fields.

Examples:

```text
Categorization: Reality
Categories: Observed, Imagined

Categorization: PredictionStatus
Categories: Predicted, Confirmed, Rejected

Categorization: SourceType
Categories: ExternalObserved, ExpandedCandidate, ModelPredicted
```

---

## 7. The Five SAFER Operators

The primitive SAFER operators are:

```text
Source
Anchor
Factor
Expand
Refine
```

They are the primitive transition forms of the semantic algebra.

Concrete SAFER expressions are parameterized by plugin actions.

---

## 8. Source

### 8.1 Type

```text
Source: Dimension -> ExternalSource
```

More concretely:

```text
Source.Action.connect: KC[Key, Repr]
Source.run(dimension, action): ExternalSource
```

### 8.2 Semantics

`Source` introduces external computation into a semantic dimension.

It produces:

```text
ExternalSource(dimension, action.reference, compute)
```

with lineage:

```text
Lineage.Source(dimension, action.reference, None)
```

### 8.3 Role

`Source` is the entry point from external data into SAFER.

Examples:

```text
external stack observations -> Compute[Stack]
lab records                  -> Compute[Experiment]
sensor data                  -> Compute[Measurement]
user events                  -> Compute[Session]
```

---

## 9. Anchor

### 9.1 Type

```text
Anchor: Reconcile -> MemoryWithLineage
```

where:

```text
Reconcile = optional head memory + set of computes
```

The action supplies:

```text
merge:  (Repr, Repr) -> Repr
enrich: Repr -> Repr
persistSpec: PersistSpec
memorize: Persisted[Key, Repr] -> Memory
```

### 9.2 Semantics

`Anchor` is the single point of reconciliation.

It aligns an optional existing memory head with one or more compute sources of the same dimension.

Conceptually:

```text
source computations
  -> union
  -> reduce(merge)
  -> map(enrich)
  -> persist(persistSpec)
  -> memorize
  -> MemoryWithLineage
```

So the semantic structure is:

```text
Anchor(Reconcile(head, computes), action)
  = MemoryWithLineage(
      memory = action.memorize(
        persist(
          action.persistSpec,
          union(sources).reduce(action.merge).map(action.enrich)
        )
      ),
      lineage = Lineage.Anchor(align, action.reference)
    )
```

### 9.3 Role

`Anchor` is not merely persistence.

It means:

> Multiple aligned upstream computations and/or an existing memory head are reconciled into a consolidated memory for a dimension.

This is the most important consolidation boundary in SAFER.

### 9.4 Persistence Delegation

Whether Anchor updates shared streaming persistence or creates a new snapshot is not decided by the SAFER kernel.

That is delegated to:

```text
PersistSpec
memorize(...)
PersistenceContext
```

Examples:

```text
Streaming persistence:
  feed new data into shared persistence or existing head

Snapshot persistence:
  create a new persisted materialization or reference
  make rollback and version isolation easier
```

Both are valid implementations of the same semantic Anchor transition.

---

## 10. Factor

### 10.1 Type

```text
Factor: MemoryWithLineage -> Map[Dimension, ComputeWithLineage]
```

The action supplies:

```text
decompose: Repr -> Map[Dimension, Seq[(Key, Repr)]]
```

### 10.2 Semantics

`Factor` decomposes a memorized dimension into one or more target dimensions.

Conceptually:

```text
Memory[A]
  -> dataPlan
  -> flatMap(decompose)
  -> split by target Dimension
  -> ComputeWithLineage for each target Dimension
```

For each target dimension `B`, the output lineage is:

```text
Lineage.Factor(parentMemoryLineage, B, action.reference)
```

### 10.3 Role

`Factor` is semantic decomposition.

Examples:

```text
Stack      -> Block
Block      -> Color
Block      -> Size
Experiment -> Material
Experiment -> Measurement
Document   -> Entity
Session    -> Event
```

`Factor` does not consolidate the result. It produces keyed computation. A later `Anchor` can memorize the result.

---

## 11. Expand

### 11.1 Type

```text
Expand: (MemoryWithLineage, MemoryWithLineage, Dimension) -> ComputeWithLineage
```

The action supplies:

```text
compose: (Repr, Repr) -> Seq[(Key, Repr)]
```

### 11.2 Semantics

`Expand` composes two memorized inputs into a target dimension.

Conceptually:

```text
Memory[A] x Memory[B]
  -> join(dataPlanA, dataPlanB)
  -> flatMap(compose)
  -> Compute[C]
```

The output lineage is:

```text
Lineage.Expand(left.lineage, right.lineage, targetDimension, action.reference)
```

### 11.3 Role

`Expand` is semantic composition.

Examples:

```text
TopBlock + BottomBlock -> CandidateStack
Material + Protocol    -> CandidateExperiment
User + Offer           -> CandidateRecommendation
Object + Relation      -> Scene
```

Expand may produce derived, imagined, or candidate state. Whether and how that state is later memorized is determined by Anchor and the persistence/memory specs.

### 11.4 Why Expand Does Not Need Built-in Filters

Expand should remain a clean composition operator.

It does not need parameters such as:

```text
leftFilter
rightFilter
```

Filtering and selection are represented by `Refine`.

If a caller wants a more specific or efficient composition, it should feed already-refined memory paths into Expand:

```text
Expand(Memory[Block | Heavy], Memory[Block | Light], composeStack)
```

or:

```text
Expand(Memory[Stack | MostRecent100], Memory[Rule | Active], compose)
```

This keeps the algebra clean:

```text
Refine first, then Expand.
```

Instead of:

```text
Expand with embedded filters.
```

This is analogous to relational systems, where filtered inputs can be fed into a join rather than making every join operator carry arbitrary filtering policies.

---

## 12. Refine

### 12.1 Type

```text
Refine: (MemoryWithLineage, Categorization) -> Map[Category.Name, MemoryWithLineage]
```

The action supplies:

```text
categorize: Repr -> Category.Name
persistSpec: PersistSpec
memorize: Persisted[Key, Repr] -> Memory
```

### 12.2 Semantics

`Refine` partitions an existing memory by category and caches/materializes each category as memory.

Conceptually:

```text
parent.memory.persisted.dataPlan
  -> split(categorize)
  -> persist each category using persistSpec
  -> memorize each persisted category
  -> return Map[Category.Name, MemoryWithLineage]
```

Each category memory has lineage:

```text
Lineage.Refine(parent.lineage, Category(categoryName, categorization), action.reference)
```

and parent pointer:

```text
parent = Some(parentMemory)
```

### 12.3 Role

`Refine` is not a new reconciliation step.

It does not merge conflicting computes. It does not replace Anchor. It is a categorized memory view/materialization over an already memorized state.

The exact nature of the categorized memory is delegated to the persistence and memory specs:

```text
lazy or eager
short-lived or long-lived
streaming or snapshot
cache or durable materialization
compressed or uncompressed
prediction-enhanced or plain
```

### 12.4 Categorization as Generalized Filtering

Refine is a generalized filtering/materialization operator.

It applies an exclusive categorization to an existing memory and returns a map from category name to category memory.

Each categorization defines a total, single-valued partitioning rule:

```text
categorize: Repr -> Category.Name
```

Within one categorization, each key belongs to exactly one category.

Overlapping or multi-label use cases are represented by multiple independent categorizations, not by multi-label categories inside one categorization.

A category may represent:

```text
semantic class
operational filter
sampling subset
time window
recency view
confidence band
optimization-specific subset
```

For example, "most recent 100 instances" can be represented as a category within a recency/subsampling categorization.

Therefore Refine generalizes filtering. It allows downstream operators such as Expand to operate on already-refined memory paths rather than embedding filter predicates directly in those operators.

### 12.5 Categorization Versioning

A refinement is keyed by:

```text
CategorizationVersion(categorization, action.reference)
```

This means the same parent memory can have multiple categorizations, and the same categorization can have multiple versions.

The category path becomes part of the memory path:

```text
MemoryPath = Dimension + Seq[Category]
MemoryVersionPath = Anchor + Seq[CategoryVersion]
```

This is essential for lineage-aware memory addressing.

---

## 13. Query Binding

Query is simple at the SAFER core level.

A query binds to memory by:

```text
Dimension
Key
```

For refined memory, it binds by:

```text
MemoryPath
Key
```

where:

```text
MemoryPath = Dimension + optional Category path
```

A version-aware query may bind by:

```text
MemoryVersionPath
Key
```

The actual retrieval implementation belongs to the persistence layer:

```text
Persisted.retrieval
PersistenceContext
Memory wrapper
```

So query is not a separate core algebra in the current formulation.

Core SAFER says:

```text
semantic address = Dimension/MemoryPath
instance address = Key
versioned address = MemoryVersionPath
```

The persistence layer says how to retrieve it.

Memory is addressed by path and key. SAFER does not require a general memory-materialization comparison operation as a core use case.

---

## 14. Persistence and Memory Specs

Many questions should not be answered in the SAFER kernel.

They belong to:

```text
PersistSpec
memorize(...)
PersistenceContext
Memory implementation
```

These include:

```text
lazy vs eager materialization
streaming vs snapshot persistence
shared persistence vs new persistence
cache lifetime
retention
compression
rollback
model training
predictive summaries
custom retrieval
storage backend
state compaction
```

This is a feature, not a weakness.

It keeps SAFER algebraic and portable while allowing different systems to make different operational tradeoffs.

### 14.1 Streaming vs Snapshot

A streaming persistence spec may mean:

```text
append or feed new data into shared persistence / existing head
```

A snapshot persistence spec may mean:

```text
create a new persisted materialization / new head reference
```

Streaming can be more efficient for continuous updates.

Snapshotting can make rollback, reproducibility, and version isolation easier.

Both are valid interpretations of the same SAFER transition.

---

## 15. Plugin Actions as Part of the Algebra

Plugin actions are part of concrete SAFER expressions.

They are not primitive operators, but they parameterize primitive operators.

This is the same relationship as relational algebra:

```text
select(predicate)
join(condition)
project(expression)
aggregate(function)
```

For SAFER:

```text
Source(connect)
Anchor(merge, enrich, persistSpec, memorize)
Factor(decompose)
Expand(compose)
Refine(categorize, persistSpec, memorize)
```

The primitive operator gives the algebraic transition shape.

The plugin action gives local semantic meaning and materialization behavior.

The correct distinction is:

```text
SAFER primitive kernel:
  Source, Anchor, Factor, Expand, Refine

Concrete SAFER expression:
  primitive operator + typed plugin action + lineage reference
```

The plugin actions may themselves be written in a rule language or implemented as code.

---

## 16. Dataflow Substrate

SAFER is interpreted over a lower-level keyed dataflow substrate.

The substrate includes operations such as:

```text
map
flatMap
split
reduce
union
join
persist
retrieve
```

This substrate is not the SAFER algebra itself. It is the execution algebra used to implement SAFER transitions.

Examples:

```text
Source  -> connect
Anchor  -> union + reduce(merge) + map(enrich) + persist + memorize
Factor  -> flatMap(decompose) + split(dimension)
Expand  -> join + flatMap(compose)
Refine  -> split(categorize) + persist + memorize
```

This is analogous to SQL query engines:

```text
SQL / relational algebra gives logical meaning.
Physical operators implement it.
```

SAFER gives semantic state-transition meaning. The dataflow substrate implements it.

---

## 17. Closure

SAFER is closed over its semantic object universe.

The primitive transitions produce only well-formed semantic objects:

```text
Source  -> ExternalSource / ComputeWithLineage
Anchor  -> MemoryWithLineage
Factor  -> Map[Dimension, ComputeWithLineage]
Expand  -> ComputeWithLineage
Refine  -> Map[Category.Name, MemoryWithLineage]
```

In simplified form:

```text
Source: External -> Compute[D]
Anchor: Compute[D] plus optional Memory[D] -> Memory[D]
Factor: Memory[A] -> Compute[B]
Expand: Memory[A] x Memory[B] -> Compute[C]
Refine: Memory[D] -> Map[Category, Memory[D]]
```

The operators do not produce arbitrary runtime objects. They stay inside the semantic computation/memory universe.

---

## 18. Core Laws

A formal algebra needs laws. SAFER laws are memory-oriented, category-oriented, and lineage-oriented.

### 18.1 Dimension Consistency

Aligned inputs to Anchor must share a dimension.

```text
Align(head, computes) requires all producer lineages have the same dimension.
```

This prevents invalid reconciliation across unrelated semantic dimensions.

### 18.2 Anchor Reconciliation

Anchor is the single reconciliation point.

```text
Anchor(Reconcile(head, computes), action)
```

must combine all aligned sources through:

```text
union
reduce(action.merge)
map(action.enrich)
persist
memorize
```

### 18.3 Lineage Preservation

Every transition records lineage.

```text
Source  records dimension and action reference.
Anchor  depends on aligned upstream producers.
Factor  depends on parent memory.
Expand  depends on left and right memories.
Refine  depends on parent memory.
```

Derived data should never appear without an explanatory lineage path.

### 18.4 Refine Parent Preservation

Refine preserves parent memory.

```text
Refine(parent, categorization)
```

does not destroy or replace the parent memory. It creates category materializations with:

```text
parent = Some(parent)
```

### 18.5 Refine Dimension Preservation

Refine does not change dimension.

```text
Lineage.Refine(parent, category, action).dimension = parent.dimension
```

It changes the memory path by adding a category, not by creating a new base dimension.

### 18.6 Categorization Exclusivity

For a given categorization version over a memory, each key belongs to exactly one category.

```text
categorize: Repr -> Category.Name
```

is single-valued within that categorization.

Overlapping or multi-label views are represented by multiple categorizations, not by multiple categories inside one categorization.

### 18.7 Categorization as Filtering

A category memory is a materialized filtered view of its parent memory.

It preserves the parent dimension and lineage while narrowing the memory path.

```text
Memory[D | category] is still Memory[D],
but addressed by a more specific MemoryPath.
```

### 18.8 Refine Category Versioning

A refinement is identified by:

```text
CategorizationVersion(categorization, action.reference)
```

So different categorization versions are distinct materializations.

### 18.9 Compute-to-Memory Boundary

`Source`, `Factor`, and `Expand` produce compute.

`Anchor` produces reconciled memory.

`Refine` produces categorized memory materializations from existing memory.

This separation should be preserved.

### 18.10 Keyed Materialization

`KC[Key, Repr]` is logically keyed.

However, physical uniqueness and consolidation are realized at materialization boundaries, especially Anchor.

```text
logical key structure belongs to KC
semantic reconciliation belongs to Anchor
physical realization belongs to ComputationContext/PersistenceContext
```

### 18.11 Expand Is Not Generally Commutative

```text
Expand(left, right, compose) != Expand(right, left, compose)
```

unless the compose rule and identity construction are symmetric.

Role order matters in many semantic domains:

```text
top block + bottom block != bottom block + top block
```

### 18.12 Anchor and Refine Do Not Generally Commute

Generally:

```text
Refine(Anchor(compute), categorize)
!=
Anchor(Refine(compute, categorize))
```

In the current design, Refine operates on memory, not arbitrary compute, so the right-hand expression is not even a primitive SAFER form unless extended.

This prevents accidental categorization before reconciliation unless explicitly modeled.

---

## 19. Memory Addressing, Not Memory Comparison

SAFER does not require a general equivalence relation for comparing memory materializations as a core operation.

The intended use is path-based addressing:

```text
MemoryPath + Key
```

or version-aware addressing:

```text
MemoryVersionPath + Key
```

For root memory:

```text
AnchorLineage + Key
```

For refined memory:

```text
AnchorLineage + CategoryVersion path + Key
```

This supports:

```text
query binding
cache lookup
materialization addressing
rollback
lineage-aware retrieval
debugging
semantic dependency tracking
```

The system can add planner-level materialization reuse later if needed, but it is not part of the core algebraic intent.

---

## 20. Well-Formed SAFER Program

A SAFER program is a graph of transitions.

It is well-formed if:

1. Every `ComputeWithLineage` has a `Lineage.Compute`.
2. Every `MemoryWithLineage` has a `Lineage.Memorize`.
3. `Anchor` inputs align to one dimension.
4. `Factor` outputs are grouped by target dimension.
5. `Expand` declares a target dimension.
6. `Refine` declares a categorization and produces category memories under the same parent dimension.
7. Each categorization is exclusive and single-valued.
8. Multiple overlapping views are represented by multiple categorizations.
9. Every transition action has a stable reference.
10. Every memory materialization is addressable by `MemoryPath` or `MemoryVersionPath`.
11. Persistence behavior is supplied by `PersistSpec`.
12. Memory wrapper behavior is supplied by `memorize`.

This keeps the algebra independent of any particular persistence backend.

---

## 21. Scientific and Agent-Memory Interpretation

SAFER is designed for systems where memory is not just storage.

It supports memory as evolving semantic state.

Examples:

```text
Observed experiments
  -> Anchor Experiment memory
  -> Factor into Materials, Protocols, Measurements
  -> Anchor those dimensions
  -> Expand candidate Experiments
  -> Refine by Outcome, Reality, Confidence, Recency, Sampling
```

or:

```text
Observed stacks
  -> Anchor Stack memory
  -> Factor into Blocks
  -> Anchor Block memory
  -> Factor Blocks into Size and Color
  -> Expand Blocks into candidate Stacks
  -> Refine Stacks by Stability and Reality
```

This is why SAFER is relevant to AI agents and scientific discovery systems. It provides a way to turn streams of experience into structured, queryable, versioned, and compositional memory.

---

## 22. Revised Open Questions

After clarifying the implementation boundary, the real open questions are not whether SAFER should decide persistence or query semantics. Those are mostly delegated.

The better questions are:

1. What guarantees should `PersistSpec` declare?
    - retention
    - storage type
    - lazy/eager materialization
    - streaming/static behavior
    - cache lifetime
    - rollback support

2. What guarantees should `memorize` declare?
    - compression behavior
    - predictive model training
    - custom retrieval behavior
    - custom memory wrapper semantics

3. What algebraic properties must `Anchor.merge` declare?
    - associativity
    - commutativity
    - idempotence
    - monotonicity
    - order sensitivity

4. What properties must `Refine.categorize` declare?
    - totality
    - determinism
    - exclusivity
    - stability under parent memory updates

5. Which categorizations should be introduced for semantic control?
    - reality
    - confidence
    - recency
    - sampling
    - prediction status
    - operational subset
    - optimization subset

6. How should observed, imagined, predicted, and derived states be represented?
    - as dimensions when they are semantic object types
    - as categorizations/categories when they are partitions of memory
    - as meanings inside referenced program/action specs when they describe how a transition was produced

These are spec-layer and extension-layer questions, not blockers for the core SAFER algebra.

---

## 23. Formal Summary

A compact formal summary:

```text
Dimension D
Key
Repr

Compute[D] =
  KC[Key, Repr] + Lineage.Compute

Memory[D] =
  Memorized(Persisted[Key, Repr]) + Lineage.Memorize

MemoryPath =
  Dimension + Seq[Category]

MemoryVersionPath =
  AnchorLineage + Seq[CategoryVersion]
```

Primitive transitions:

```text
Source:
  Dimension x Source.Action -> ExternalSource

Anchor:
  Reconcile x Anchor.Action -> MemoryWithLineage

Factor:
  MemoryWithLineage x Factor.Action -> Map[Dimension, ComputeWithLineage]

Expand:
  (MemoryWithLineage, MemoryWithLineage, Dimension) x Expand.Action -> ComputeWithLineage

Refine:
  (MemoryWithLineage, Categorization) x Refine.Action -> Map[Category.Name, MemoryWithLineage]
```

Action rules:

```text
Source.Action:
  connect: KC[Key, Repr]

Anchor.Action:
  merge: Repr x Repr -> Repr
  enrich: Repr -> Repr
  persistSpec: PersistSpec
  memorize: Persisted[Key, Repr] -> Memory

Factor.Action:
  decompose: Repr -> Map[Dimension, Seq[(Key, Repr)]]

Expand.Action:
  compose: Repr x Repr -> Seq[(Key, Repr)]

Refine.Action:
  categorize: Repr -> Category.Name
  persistSpec: PersistSpec
  memorize: Persisted[Key, Repr] -> Memory
```

Runtime interpretation:

```text
Source  -> connect
Anchor  -> union + reduce(merge) + map(enrich) + persist + memorize
Factor  -> flatMap(decompose) + split(dimension)
Expand  -> join + flatMap(compose)
Refine  -> split(categorize) + persist + memorize
```

---

## 24. Closing Claim

SAFER is a typed, higher-order algebra for semantic state evolution.

Its primitive operators define the semantic transition kernel:

```text
Source
Anchor
Factor
Expand
Refine
```

Concrete expressions are parameterized by plugin actions. Persistence and memory behavior are delegated to specs. Physical execution is delegated to a lower-level keyed dataflow and persistence substrate.

This separation is what makes SAFER both rigorous and extensible:

```text
SAFER kernel:
  semantic transition shape

Plugin actions:
  local domain rules and action references

PersistSpec / memorize:
  materialization and memory semantics

ComputationContext / PersistenceContext:
  execution substrate
```

Categorization is not merely classification. It is a generalized filtering and materialization mechanism. Each categorization is exclusive and single-valued, while multiple categorizations provide overlapping views.

Memory is addressed by dimension/path and key, not compared by a general equivalence relation as a core operation.

Observed, imagined, predicted, and derived states do not require special built-in lineage annotations. They are represented through dimensions, categorizations/categories, and the referenced program/action specifications.

Therefore SAFER can be described as a formal algebra without forcing all storage, streaming, snapshot, compression, rollback, filtering, and model-training choices into the core. Those are valid interpretations and extensions of the algebra, not the algebra itself.
