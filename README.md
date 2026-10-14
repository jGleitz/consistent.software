# consistent.software

An ambitious project to tackle _inconsistencies_ in software development.
Our vision is that developers can easily define rules that keep artefacts consistent across technologies, programming languages and locations.

For example, a typical SaaS product maintains an OpenAPI specification, a Java server implementation and a Node.js client. All three are checked in and edited by developers. Consistency rules propagate changes in any one artefact to the others, where applicable.

In this scenario:

- All three artefacts are first-class citizens. None has to be second-rate because it is generated and cannot be adapted.
- The artefacts cannot drift apart, and no valuable resources such as developer attention or LLM tokens are spent on keeping them in sync.

## Layers

We build towards our vision by stacking the following components:

- **Strong type system.** Our consistency rules need to rely on strong guarantees and well-defined semantics about the data they are working with. We found no existing system that offers what we need, so we are developing our own.
- **Strong meta-model.** We establish clear semantics for model data. This allows us to integrate diverse sources of data while retaining the ability to write generic rules about them.
- **Easy-to-use consistency rules.** We provide a simple, declarative language for writing consistency rules. This allows developers to express their consistency requirements in a clear and concise manner, without having to worry about the underlying implementation details.

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md).
