---
title: Metamodel
---
<!--@include: ./math-definitions.md-->
# Metamodel

::: info Goal
Define an architecture to represent a breadth of real-world entities as *structured data* (models).
Specify *four operations* to act on models: create, read, update and delete (CRUD).
Design a rich system of types, costraints and invariants, such that any CRUD operation always yields a *semantically correct* model.
:::

We will define the following concepts:
 * [primitive data](#primitive-data)
 * models
 * identifiers
 * references

## Types

The metamodel knows types.
Types are sets representing possible values some expression can take.
We denote the set of all types $\types$, which is a set of sets.
We will define $\types$ throughout this document.

### Empty Type

The empty set $\emptyset$ is a type, representing the fact that an expression cannot take any valid value.
An expression having $\emptyset$ as its type is an error.
$$
    \emptyset\in\types
$$

### Union Types

The <dfn>union type</dfn> $T_1\cup T_2$ of two types $T_1$ and $T_2$ is a type, representing _either_ $T_1$ or $T_2$:
$$
    \forall T_1,T_2\in\types\colon \left(T_1\cup T_2\right)\in\types
$$

### Intersection Types

The <dfn>intersection type</dfn> $T_1\cap T_2$ of two types $T_1$ and $T_2$ is a type, representing _both_ $T_1$ and $T_2$:
$$
\forall T_1,T_2\in\types\colon \left(T_1\cap T_2\right)\in\types
$$

## Primitive Data

A primitive datum is the atomic building block out of which models are specified.
‘Atomic’ in the sense that a primitive datum is not composed of something else, and that it is indivisible.

:::info Not Extensible
We currently don’t plan for the types of primitive data to be extensible.
Instead, the available primitive data types are baked into the metamodel.
We might revise this decision if a compelling use case for externally defined primitive data types is found.
:::

The metamodel knows these primitive types:

Numbers
: A number is any [rational number](https://en.wikipedia.org/wiki/Rational_number).
We denote the set of primitive numbers $\tNumber$.

Strings
: A string is a finite [sequence](https://en.wikipedia.org/wiki/Sequence) of [unicode characters](https://www.unicode.org/versions/Unicode17.0.0/core-spec/chapter-2/#G8085).
We denote the set of primitive strings $\tString$.

Tags
: Tags are well-known values, distinct from other primitive data.
We denote the set of primitive tags $\tTag$.

Any primitive datum forms a type:

$$
\begin{align}
    \forall n\in\tNumber &\colon \left\{n\right\}\in\types \\
    \forall s\in\tString &\colon \left\{s\right\}\in\types \\
    \forall t\in\tTag &\colon \left\{t\right\}\in\types \\
\end{align}
$$

## Models

### Labels

A label asrcibes meaning to a datum.
There are two types of labels:

Value-based labels
: A [sequence](https://en.wikipedia.org/wiki/Sequence) of [unicode characters](https://www.unicode.org/versions/Unicode17.0.0/core-spec/chapter-2/#G8085).

Reference-based labels
:  

We denote the set of labels $\labels$.

::: tip Not Strings
Labels are distinct from primitive strings!
An label also does not form a type.
:::

### Model Part

A model part assigns labels to data, thereby describing the data’s meaning.
Formally, we define a model part as a function into $\types$ whose domain is a non-empty, finite set of labels.
We denote the set of all model parts $\modelparts$.

$$
    \modelparts \coloneqq \left\{L\rightarrow\types \:\mid\: L \subset\labels \,\land\, \lvert L\rvert \in\mathbb{N}^+ \right\}
$$




