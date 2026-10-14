# Documentation

This document sets out where each piece of documentation belongs and how to write it.
It applies to everyone who changes this repository, humans and agents alike.

## Where Documentation Goes

Internal documentation lives as close as possible to the code it describes.
Every document has one target audience, and its location tells readers who that is:

| Location                     | Audience                     | Content                                        |
| ---------------------------- | ---------------------------- | ---------------------------------------------- |
| `README.md`                  | Clients of the directory     | What the directory provides and how to use it  |
| `ARCHITECTURE.md`            | Maintainers of the directory | How the directory works and how to maintain it |
| Code comments and KDoc       | Readers of that code         | What is not obvious from the code itself       |
| [`documentation`][site]      | Users of consistent.software | User-facing documentation, published as a site |
| [`CONTRIBUTING.md`][contrib] | Contributors                 | How to contribute                              |
| [`contributing/`][dir]       | Maintainers                  | Topics that span the whole repository          |
| `AGENTS.md`                  | AI agents                    | Rules that apply only to agents                |

[site]: ../documentation/
[contrib]: ../CONTRIBUTING.md
[dir]: index.md

### `contributing/`

[`index.md`](index.md) lists every document in `contributing/` and summarises which information it contains.
When you add, remove, or change the scope of a document there, update the index.

### `README.md`

A directory’s `README.md` is written for its _clients_: whoever uses what the directory provides.
For the repository root and for libraries, clients are users of consistent.software.
For internal modules, clients are maintainers who use the module to work on the wider project.
In both cases, the same principles apply: the README contains only client-facing information, written in client-facing language.
It explains what the directory is for and how to use it, not how it works inside.

### `ARCHITECTURE.md`

A directory’s `ARCHITECTURE.md` is written for those who maintain _that directory_.
It explains how the directory is structured and how to work on it, for example its test strategy.
The root [`ARCHITECTURE.md`](../ARCHITECTURE.md) gives an overview of the whole repository.

### The Documentation Site

The [`documentation`](../documentation/) module builds a VitePress site.
It contains user-facing documentation only; internal documentation never goes there.

### `AGENTS.md`

`AGENTS.md` files contain only rules that apply to AI agents but not to humans.
Everything human contributors need, agents need too, so it belongs in the documentation for humans.
Agent harnesses load `AGENTS.md` files automatically, but not other documents.
The root [`AGENTS.md`](../AGENTS.md) therefore tells agents where to find which information, so that they can look it up when it is relevant.

## How to Write Documentation

### Write for the Target Audience

Before you write, be clear about who will read the document, what they already know, and what they need from it.
Write for exactly that audience.
If a piece of content serves a different audience, it belongs in a different document.

### Use British English

All documentation is written in British English.

### Weigh Usefulness Against Cost

Every piece of documentation has a cost: it needs to be kept up to date, and it drowns out other information.
Document something only if its usefulness outweighs that cost.

For internal documentation:

- If maintainers can find the information well by reading the relevant code, don’t document it.
- Otherwise, first consider improving the code, or the documentation of where which code lives, so that maintainers can find the information there.

If you do document something, think about how to keep the burden of maintaining it low.
For example, link to information instead of repeating it, and refer to code by the names of its symbols rather than restating what it does.
