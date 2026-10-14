# Architecture

This document gives maintainers an overview of the whole repository.
Directories may have their own `ARCHITECTURE.md` that describes their internals.
See [Documentation](contributing/documentation.md) for where which piece of documentation goes.

## Code Map

The most important directories are:

- `┌── `[`📁 build-logic`](./build-logic/) — Defines the majority of the Gradle build.
- `├── `[`📁 data`](./data/) — The data we support and its types.
- `├── `[`📁 documentation`](./documentation/) — Builds the VitePress site with our user-facing documentation.
- `└── `[`📁 library`](./library/) — Helper libraries we develop.
