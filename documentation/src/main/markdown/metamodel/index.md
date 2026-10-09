---
title: Metamodel
---

<!--@include: ./math-definitions.md-->

# Metamodel

::: info Goal
Define an architecture to represent a breadth of real-world entities as _structured data_ (models).
Specify _two operations_ to act on models: reading and writing.
Design a rich system of types, costraints, and invariants, such that any operation always yields a _semantically correct_ model.
:::

We will define the following concepts:

- [values](#values)
- models
- identifiers
- references

## Labels

Going forward, we will need to assign labels to values.
To be able to refer to labels in mathematical notation, we define the set $\labeldomain$ of all labels.
By convention, we will understand strings in formulas typset in monospace font to be elements of $\labeldomain$, like $\label{name}$ or $\label{id}$.

## Values

We denote the set of all model values $\valuedomain$. There are different kinds of values:

<!-- @formatter:off -->
Numbers
: A number is any of the [rational numbers](https://en.wikipedia.org/wiki/Rational_number), which we denote $\numberdomain$.

  Examples:
    - $-3$
    - $0$
    - $42$
    - $\frac{11}{3}$
    - $9\,223\,372\,036\,854\,775\,808$
    {.inline}

<!-- Strings-->
<!--: A string is a finite [sequence](https://en.wikipedia.org/wiki/Sequence) of [assigned code points in Unicode 17.0.0](https://www.unicode.org/versions/Unicode17.0.0/core-spec/chapter-2/#G18444).-->
<!--We denote the set of string values $\stringdomain$.-->

Bags
: A bag is a finite [multiset](https://en.wikipedia.org/wiki/Multiset) of values.
  We denote the set of bags over a set $X$ as $\bags{X}$.

  Examples:
    - $\left\{1, 2, 1, 2\right\}$
    - $\left\{42\right\}$
    - $\left\{\right\}$
    - $\left\{\left\{0, \left\{0\right\}, 0 , 1\right\}, \left\{1, 1\right\}, 0\right\}$
    {.inline}

Lists
: A list is a finite [sequence](https://en.wikipedia.org/wiki/Sequence) of values.
  We denote the set of lists over a set $X$ as $\lists{X}$.

  Examples:
    - $\left(1, 2, 3, 4\right)$
    - $\left(5, 0, 5\right)$
    - $\left(\right)$
    - $\left(\left(1\right), 0, \left\{1\right\}\right)$
  {.inline}

Records
: Records assign [labels](#labels) to values.
  They are finite, partial functions from labels to values.
  We denote the set of records over a set $X$ as $\records{X}$.

  Examples:
    - $\left[\label{count}\mapsto 42\right]$
    - $\left[\label{dice\_rolls}\mapsto \left\{3, 3, 1, 6\right\},\,\label{ball\_picks}\mapsto \left(0,0, 1,1,0\right)\right]$

<!-- @formatter:on -->

### Layered Construction

The value kinds above are mutually recursive: bags, lists, and records contain values, which themselves may be bags, lists, or records. To give this a well-defined foundation, we construct $\valuedomain$ in layers.

**Definition (Value Hierarchy).** We define an ascending sequence of sets $(\layer{n})_{n \in \mathbb{N}}$ by induction:

$$
\begin{aligned}
  \layer{0} &\coloneqq \numberdomain \\
  \layer{n+1} &\coloneqq \layer{n} \;\sqcup\; \bags{\layer{n}} \;\sqcup\; \lists{\layer{n}} \;\sqcup\; \records{\layer{n}}
\end{aligned}
$$

Each layer $\layer{n+1}$ contains all values from the previous layer $\layer{n}$, plus all bags, lists, and records formed from those values.

**Definition (Value Domain).** The set of all values is the union of all layers:

$$
\valuedomain \coloneqq \bigcup_{n \in \mathbb{N}} \layer{n}
$$

**Theorem (Fixed Point).** $\valuedomain$ satisfies the equation:

$$
\valuedomain = \numberdomain \;\sqcup\; \bags{\valuedomain} \;\sqcup\; \lists{\valuedomain} \;\sqcup\; \records{\valuedomain}
$$

_Proof sketch._

- ($\supseteq$): Numbers are in $\layer{0} \subseteq \valuedomain$. Any bag, list, or record over $\valuedomain$ involves finitely many values, all lying in some layer $\layer{N}$. Thus it belongs to $\layer{N+1} \subseteq \valuedomain$.
- ($\subseteq$): By induction, each $\layer{n} \subseteq \numberdomain \sqcup \bags{\valuedomain} \sqcup \lists{\valuedomain} \sqcup \records{\valuedomain}$, so their union $\valuedomain$ is contained in the right-hand side. ∎

**Theorem (Well-Foundedness).** Every value $v \in \valuedomain$ has a unique _minimal layer_ $\ell(v) \in \mathbb{N}$, the least $n$ such that $v \in \layer{n}$. If $v$ is a compound value (bag, list, or record), all its immediate constituents have strictly smaller layer: $\ell(\text{constituent}) < \ell(v)$.

This ensures the [axiom of regularity](https://en.wikipedia.org/wiki/Axiom_of_regularity): there are no infinite descending chains of membership.

## Models

### Model Part

A model part assigns labels to data, thereby describing the data’s meaning.
Formally, we define a model part as a function into $\types$ whose domain is a non-empty, finite set of labels.
We denote the set of all model parts $\modelparts$.

$$
    \modelparts \coloneqq \left\{L\rightarrow\types \:\mid\: L \subset\labels \,\land\, \lvert L\rvert \in\mathbb{N}^+ \right\}
$$

<!--@include: ./type-system.md-->
