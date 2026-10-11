# consistent.software

An ambitious project to tackle _inconsistencies_ in software development.
Our vision is that developers can easily define rules that keep artefacts consistent across technologies, programming languages and locations.

For example, a classical SaaS service maintains an OpenAPI specification, a Java server implementation and Node.js client. All are checked in, all are edited by devs. Consistency rules transfer changes in either artefact to the others, if applicable.

In this scenario:
  * All three artefacts are first-class citizens. None needs to be subpar because it’s generated and cannot be adapted.
  * The artefacts cannot drift, and no valuable resources like developer attention or LLM tokens are wasted to achieve it.


## Layers

We built towards our vision by stacking the following components:

  * **Strong type system.** Our consistency rules need to rely on strong guarantees and well-defined semantics about the data they are working with. We found no pre-existing system offering what we needed and, so, develop our own.
  * **Strong meta-model.** We establish clear semantics for model data. This allows us to integrate diverse sources of data while but also retain the ability to write generics rules about it.
  * **Easy-to-use consistency rules.** We provide a simple, declarative language for writing consistency rules. This allows developers to express their consistency requirements in a clear and concise manner, without having to worry about the underlying implementation details.


## Repository Overview

The most important directories are:

 * `┌── `[` 📁 build-logic`](./build-logic/) — Defines the majority of the gradle build.
 * `├── `[`📁 documentation`](./documentation/) — A VitePress site for both user-facing and internal documentation.
 * `├── `[`📁 library`](./library/) — Helper libraries we develop.
 * `├── `[`📁 data`](./data/) — The data and its types we support.
