---
title: Metamodel
---

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

## Primitive Data

A primitive datum is the atomic building block out of which models are specified.
‘Atomic’ in the sense that a primitive datum is not composed of something else, and that it is indivisible.

:::info Not Extensible
We currently don’t plan for the types of primitive data to be extesible.
Instead, the available primitive data types are baked into the metamodel.
We might revise this decision if a compelling use case for externally-defined primitive data types is found.
:::

The metamodel knows these primitive types:

### Numbers

A number is any [real number](https://en.wikipedia.org/wiki/Real_number).

### Strings

A string is a finite [sequence](https://en.wikipedia.org/wiki/Sequence) of [unicode characters](https://www.unicode.org/versions/Unicode17.0.0/core-spec/chapter-2/#G8085).




