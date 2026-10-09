# Type System

## Overview

A type system classifies values into types, enabling static reasoning about program behavior. In our metamodel, types are defined as subsets of the value domain $\valuedomain$, and subtyping is derived from set inclusion.

## Type Syntax

We define the set of types $\types$ inductively as the smallest set closed under the following constructors:

$$
\begin{aligned}
\types \coloneqq{} & \tnum \mid \tint \mid \tsingleton{v} \mid \tbag{\types} \mid \tlist{\types}{\types} \\
& \mid \tcons{\types}{\types} \mid \trecord{l_1 \mapsto \types, \ldots, l_n \mapsto \types} \mid \tclosedrecord{l_1 \mapsto \types, \ldots, l_n \mapsto \types} \\
& \mid \tinterval{a}{b}{\text{incl}_1}{\text{incl}_2} \mid \tmult{\types}{\types} \\
& \mid \tunion{\types}{\types} \mid \tintersection{\types}{\types} \mid \tsetminus{\types}{\types} \\
& \mid \trec{\alpha}{\types} \mid \tmurec{\alpha_1, \ldots, \alpha_n}{\tau_1, \ldots, \tau_n}
\end{aligned}
$$

where:

- $v \in \valuedomain$
- $l_1, \ldots, l_n \in \labeldomain$ are distinct labels
- $a, b \in \mathbb{Q} \cup \{-\infty, +\infty\}$
- $\text{incl}_1, \text{incl}_2 \in \{\text{open}, \text{closed}\}$
- $\alpha, \alpha_1, \ldots, \alpha_n$ are type variables
- $\tau_1, \ldots, \tau_n$ are types that may reference the bound type variables

### Primitive Types

- $\tnum$: All rational numbers, $\semantics{\tnum} = \mathbb{Q}$
- $\tint$: All integers, $\semantics{\tint} = \mathbb{Z}$

### Singleton Types

For each value $v \in \valuedomain$, the singleton type $\tsingleton{v}$ contains exactly that value:

$$\semantics{\tsingleton{v}} = \{v\}$$

### Parameterized Types

**Bag type:** $\tbag{T}$ represents bags where every element conforms to type $T$:

$$\semantics{\tbag{T}} = \bags{\semantics{T}}$$

**List type:** $\tlist{T}{L}$ represents lists where every element conforms to type $T$ and the length belongs to type $L$:

$$\semantics{\tlist{T}{L}} = \{ l \in \lists{\valuedomain} \mid \forall i \in \{0, \ldots, |l|-1\}.\, l[i] \in \semantics{T} \land |l| \in \semantics{L} \}$$

where $L$ is a type representing non-negative integers (e.g., an interval type).

**Record type:** $\trecord{l_1 \mapsto T_1, \ldots, l_n \mapsto T_n}$ represents open records with specified key types:

$$\semantics{\trecord{l_1 \mapsto T_1, \ldots, l_n \mapsto T_n}} = \{ r \in \records{\valuedomain} \mid \forall i \in \{1, \ldots, n\}.\, r(l_i) \in \semantics{T_i} \}$$

Open records support width subtyping: a record with additional keys is a subtype.

**Closed record type:** $\tclosedrecord{l_1 \mapsto T_1, \ldots, l_n \mapsto T_n}$ represents records with exactly the specified keys:

$$\semantics{\tclosedrecord{l_1 \mapsto T_1, \ldots, l_n \mapsto T_n}} = \{ r \in \records{\valuedomain} \mid \text{dom}(r) = \{l_1, \ldots, l_n\} \land \forall i \in \{1, \ldots, n\}.\, r(l_i) \in \semantics{T_i} \}$$

### Sequence Types

**Cons type:** $\tcons{A}{B}$ represents the concatenation of two sequence types:

$$\semantics{\tcons{A}{B}} = \{ \text{concat}(a, b) \mid a \in \semantics{A} \land b \in \semantics{B} \}$$

This constructor requires that $A$ and $B$ are sequence types (bags or lists). The concatenation operation $\text{concat}$ is defined for compatible sequence kinds.

### Interval Types

Interval types represent subsets of $\mathbb{Q}$ bounded by rational numbers or infinity:

$$\semantics{\tinterval{a}{b}{\text{incl}_1}{\text{incl}_2}} = \{ q \in \mathbb{Q} \mid a \leq q \leq b \}$$

with the constraint that inclusive bounds must be finite: if $\text{incl}_1 = \text{closed}$, then $a \in \mathbb{Q}$; if $\text{incl}_2 = \text{closed}$, then $b \in \mathbb{Q}$.

Notation: $[a, b]$ for both closed, $(a, b)$ for both open, $[a, b)$ for left closed and right open, $(a, b]$ for left open and right closed.

### Numeric Types

**Multiplication type:** $\tmult{A}{B}$ represents the set of all products of values from $A$ and $B$:

$$\semantics{\tmult{A}{B}} = \{ a \times b \mid a \in \semantics{A} \land b \in \semantics{B} \}$$

This constructor requires that $A$ and $B$ are numeric types (subtypes of $\tnum$).

### Set Operations

**Union:** $\tunion{T_1}{T_2}$ represents the union of two types:

$$\semantics{\tunion{T_1}{T_2}} = \semantics{T_1} \cup \semantics{T_2}$$

**Intersection:** $\tintersection{T_1}{T_2}$ represents the intersection of two types:

$$\semantics{\tintersection{T_1}{T_2}} = \semantics{T_1} \cap \semantics{T_2}$$

**Set difference:** $\tsetminus{T_1}{T_2}$ represents the set difference:

$$\semantics{\tsetminus{T_1}{T_2}} = \semantics{T_1} \setminus \semantics{T_2}$$

## Recursive Types

### Single Recursive Types

A recursive type $\trec{\alpha}{\tau}$ binds a type variable $\alpha$ within the body $\tau$, allowing $\tau$ to reference the type being defined.

**Semantics:** The semantic function maps recursive types to the least fixed point of a recursive equation:

$$\semantics{\trec{\alpha}{\tau}} = \text{lfp}\left(X \mapsto \semantics{\tau[X/\alpha]}\right)$$

where $\text{lfp}$ denotes the least fixed point, and $\tau[X/\alpha]$ denotes substitution of $X$ for $\alpha$ in $\tau$.

**Example:** A tree type:

$$\text{Tree} = \trec{\alpha}{\trecord{\label{value} \mapsto \tint, \label{children} \mapsto \tbag{\alpha}}}$$

The semantic equation is:

$$X = \{ r \in \records{\valuedomain} \mid r(\label{value}) \in \mathbb{Z} \land r(\label{children}) \in \bags{X} \}$$

The least fixed point is the set of all finite trees with integer values.

**Base cases:** A recursive type is inhabited if and only if its semantic equation has a non-empty least fixed point. This requires the type to have base cases that allow the recursion to terminate with finite, well-founded values.

**Example (inhabited):** $\trec{\alpha}{\trecord{\label{child} \mapsto \tunion{\alpha}{\tsingleton{\text{null}}}}}$ has base case $\text{null}$, so it's inhabited by all finite chains ending in $\text{null}$.

**Example (uninhabited):** $\trec{\alpha}{\trecord{\label{child} \mapsto \alpha}}$ has no base case, so its least fixed point is $\emptyset$.

### Mutually Recursive Types

Mutually recursive types are defined as a simultaneous fixed point of multiple type equations:

$$\tmurec{\alpha_1, \ldots, \alpha_n}{\tau_1, \ldots, \tau_n}$$

**Semantics:** The semantic function maps this to the least fixed point of the system of equations:

$$\semantics{\tmurec{\alpha_1, \ldots, \alpha_n}{\tau_1, \ldots, \tau_n}} = \text{lfp}\left((X_1, \ldots, X_n) \mapsto (\semantics{\tau_1[\vec{X}/\vec{\alpha}]}, \ldots, \semantics{\tau_n[\vec{X}/\vec{\alpha}]})\right)$$

**Example:** Doubly-linked list nodes:

$$\tmurec{\alpha, \beta}{\trecord{\label{value} \mapsto \tint, \label{next} \mapsto \tunion{\beta}{\tsingleton{\text{null}}}}, \trecord{\label{value} \mapsto \tint, \label{prev} \mapsto \tunion{\alpha}{\tsingleton{\text{null}}}}}$$

This defines two types $\alpha$ and $\beta$ that reference each other.

## Subtyping

Subtyping is derived from set inclusion of the semantic function:

$$T_1 \leq T_2 \iff \semantics{T_1} \subseteq \semantics{T_2}$$

**Examples:**

- $\tint \leq \tnum$ because $\mathbb{Z} \subseteq \mathbb{Q}$
- $\tsingleton{42} \leq \tint$ because $\{42\} \subseteq \mathbb{Z}$
- $\trecord{\label{a} \mapsto \tint, \label{b} \mapsto \tstring} \leq \trecord{\label{a} \mapsto \tint}$ (width subtyping for open records)
- $\tunion{\tint}{\tstring} \leq \tunion{\tnum}{\tstring}$ because $\mathbb{Z} \cup \mathbb{S} \subseteq \mathbb{Q} \cup \mathbb{S}$

## Type Equivalence

Two types are equivalent if they have the same semantics:

$$T_1 \equiv T_2 \iff \semantics{T_1} = \semantics{T_2}$$

## Typing Relation

A value $v$ has type $T$ if and only if $v$ belongs to the semantic interpretation of $T$:

$$v : T \iff v \in \semantics{T}$$
