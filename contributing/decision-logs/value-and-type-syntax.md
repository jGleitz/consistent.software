# Value and type syntax

Status: evolving decision log. Consolidated on 15 October 2026.
Audience: maintainers designing the external language.

This log records the agreed surface syntax and why it was chosen. It also keeps
unresolved design work visible without turning examples into language rules.
It is not a complete grammar or a statement of parser implementation status.

## Contents

- [Current syntax at a glance](#current-syntax-at-a-glance)
- [Design principles](#design-principles)
- [Decisions](#decisions)
  - [D1 — Decimal numeric literals](#d1--decimal-numeric-literals)
  - [D2 — Literals also denote singleton types](#d2--literals-also-denote-singleton-types)
  - [D3 — Intersection and union](#d3--intersection-and-union)
  - [D4 — Relative difference, without unary negation](#d4--relative-difference-without-unary-negation)
- [Symbol table: syntax to semantics](#symbol-table-syntax-to-semantics)
- [Open design areas](#open-design-areas)
  - [O1 — Maps, records, lists and bags](#o1--maps-records-lists-and-bags)
  - [O2 — Type-directed literal parsing](#o2--type-directed-literal-parsing)
  - [O3 — Remaining syntax inventory](#o3--remaining-syntax-inventory)
- [Maintenance](#maintenance)

## Current syntax at a glance

**Fixed** means an explicit design decision; **provisional** means a direction or
candidate; **open** means no concrete spelling has been agreed.

| Use case                              | Syntax / rule                                             | Status and detail                                                                                                                              |
| ------------------------------------- | --------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| Numeric literals                      | `42`, `12.34`, `.5`, `5.`, `12.34 E -2`, `1_000`, `1 000` | Fixed; decimal notation, optional uppercase `E` with signed integer exponent; ignore `_` and U+0020 SPACE; [D1](#d1--decimal-numeric-literals) |
| Singleton type                        | The literal of its sole value, e.g. `42`                  | Fixed; type/value position supplies the interpretation; [D2](#d2--literals-also-denote-singleton-types)                                        |
| Intersection / AllOf                  | `A & B`                                                   | Fixed; values belonging to both types; [D3](#d3--intersection-and-union)                                                                       |
| Union / AnyOf                         | <code>A &#124; B</code>                                   | Fixed; values belonging to either type, including both                                                                                         |
| Grouping                              | <code>(A &#124; B) &amp; C</code>                         | Fixed for these type expressions; `&` binds more tightly than <code>&#124;</code>                                                              |
| Difference                            | `A &! B`                                                  | Fixed single infix operator; precedence and associativity open; [D4](#d4--relative-difference-without-unary-negation)                          |
| Unary type complement                 | No `!T` or `~T`                                           | Deliberately absent; if a top type is exposed, use `Top &! T`                                                                                  |
| Composite syntax and other constructs | Not yet fixed                                             | See [open design areas](#open-design-areas)                                                                                                    |

Names such as `Number`, `Integer`, `Person`, `Bool` and `Top` below describe roles;
they do not establish built-in names. Composite examples in open sections are
illustrative candidates, not productions of an agreed grammar.

## Design principles

1. Prefer notation familiar from C++, Java, Kotlin and TypeScript where it fits
   the semantics. Compare Rust and Go as additional practical reference points.
2. Let a type resemble the values or uses it describes. This motivates shared
   literal and structural syntax. Set operators on types are outside this
   correspondence: they combine sets of values, not individual values. The union
   of two different singleton types contains two values, and their intersection
   is empty; neither produces a corresponding single value.
3. Prefer a small number of orthogonal constructs. Reserve concise operators for
   fundamental, frequent operations rather than inventing synonyms.
4. Check every new spelling against the symbol table. Record context, precedence,
   associativity and desugaring explicitly; do not infer them from appearance.
5. Distinguish the surface language from the internal type algebra. Types denote
   sets of values; internal negations or normal forms need not become source syntax.

## Decisions

### D1 — Decimal numeric literals

**Decision.** Use decimal digits, at most one `.` in the significand, and an
optional suffix `E<n>`. The exponent is an optionally signed decimal integer
without a decimal point. It scales the significand by ten to that power.
The significand must contain at least one digit; its point may be leading or
trailing. Uppercase `E` is the only exponent marker currently defined.

Inside a literal, `_` and U+0020 SPACE are ignored. Tabs, line breaks, non-breaking
spaces and other whitespace are not internal numeric separators.

```text
42
3.1415
.5
5.
1_000_000
1 000 000
12.34E2
12.34E-2
12.34 E -2
```

Thus `1_000`, `1 000` and `1000` denote the same number; `12.34E-2` denotes
`0.1234`. A lone `.`, a significand with two points, and a fractional exponent
such as `1E2.5` are not numeric literals.

**Why.** Decimal notation and scientific exponents are familiar and concise.
Grouping improves readability without changing the value. Restricting whitespace
to SPACE keeps this permission from silently admitting line breaks or tabs.
Leading and trailing points are intentionally accepted conveniences. They also
leave lexical conflicts for the eventual grammar: `.5` must be distinguished
from dot-prefixed constructs, while `5.foo` and `1..2` could conflict with member
access and range notation. Accepting `.5` and `5.` therefore constrains the design
of those constructs; their spelling and tokenisation are not settled here.

**Limits.** The decision does not choose machine floating-point semantics,
rounding, numeric type names or suffixes. Decimal is the only base currently
defined; hexadecimal and binary are not established syntax. Whether a leading
sign belongs to the literal or is a prefix operator remains open. Exact separator
placement and token boundaries also need a grammar: removing ignored characters
explains the value of an already delimited literal, not where that literal ends.
In particular, whitespace alone cannot separate two numeric elements because
`1 2` denotes `12`. The grammar must also distinguish the exponent in
`12.34 E -2` from a separate identifier `E` followed by subtraction, if those
constructs are introduced. These are boundary obligations for the grammar,
not provisional numeric spellings.

**Other languages.** These are lexical comparisons, not claims of equal numeric models.

| Language   | Relevant convention                                                                                                                                                                                         |
| ---------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Kotlin     | Decimal real literals use `e`/`E` and allow `_`; `.5` is allowed, but a bare trailing point is not a real literal. Internal spaces are not numeric separators. See [real literals][kotlin-literals].        |
| TypeScript | ECMAScript numeric syntax allows `.5`, `5.`, `e`/`E` and restricted `_` separators, without internal spaces. See the [ECMAScript grammar][ecma-lexical] and [TypeScript numeric separators][ts-separators]. |
| Rust       | Allows decimal exponents and `_`; `5.` can be a float literal, while `.5` is not one. Whitespace separates tokens. See [Rust tokens][rust-tokens].                                                          |
| Go         | Allows `.5`, `5.`, `e`/`E` and restricted `_` separators, without internal spaces. See [floating-point literals][go-spec].                                                                                  |

SPACE grouping is therefore a deliberate departure from all four reference
languages, with consequences for collection separators and formatting.

### D2 — Literals also denote singleton types

**Decision.** Every value-literal spelling also denotes the type containing
exactly that value when used in a type position. There is no singleton marker.
For example, `42` is a number in a value position and the type containing only
that number in a type position. This rule applies to future literal forms too;
it does not itself fix their spellings.

**Why.** The notation directly realises the principle that types resemble values.
A wrapper such as `Singleton<42>` would repeat information supplied by the context.
The surrounding grammar must therefore distinguish type and value positions;
a lexer cannot make this distinction from the literal alone.

**Other languages.**

| Language   | Singleton or nearest equivalent                                                                                                                                                                                                           |
| ---------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Kotlin     | No general source-level `42` singleton type. Its internal integer-literal types describe admissible integer types, not singleton values. An `object` declaration is a different notion of singleton. See the [type system][kotlin-types]. |
| TypeScript | Literal types such as `42`, `"ready"` and `true` use value spelling in type positions. Widening can still infer a broader type for a value. See [literal types][ts-everyday].                                                             |
| Rust       | No general value-literal type. Const arguments such as `Buffer<42>` parameterise a type by a value; they do not make `42` itself a type. See [generic parameters][rust-generics].                                                         |
| Go         | Constants are values, possibly untyped; `42` is not a type expression. See [constants and types][go-spec].                                                                                                                                |
| CDuce      | Basic and constructed data values share notation with singleton types, including pairs such as `(1,2)`. A particularly close precedent; see [types and patterns][cduce-types].                                                            |
| Ballerina  | Singleton type descriptors use constant expressions; this does not establish the same rule for every composite literal in our design. See the [specification][ballerina-spec].                                                            |

### D3 — Intersection and union

**Decision.** `A & B` is intersection (AllOf); `A | B` is union (AnyOf).
Membership in the former requires membership in both operands; membership in
the latter requires at least one. Union is inclusive, not exclusive.

```text
Positive & Integer
String | Number
A & B & C
A | B | C
A | B & C
(A | B) & C
```

`&` binds more tightly than `|`, so `A | B & C` means `A | (B & C)`.
Both operations are semantically associative, and homogeneous chains need no
parentheses. Both are also commutative and idempotent as operations on value sets;
their operand order does not select a winning alternative.

**Why these symbols.** They are compact, familiar from programmer-facing type
notation and appropriate for fundamental operations. `allOf(A, B)` and
`anyOf(A, B)` are unnecessarily wordy here. `&&` and `||` suggest Boolean
short-circuit evaluation. `∩` and `∪` are mathematically precise but less convenient
to enter and less familiar to the intended audience.

**Other languages.**

| Language   | Intersection                                                                                                           | Union                                                                                                                     |
| ---------- | ---------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| Kotlin     | Intersections are generally non-denotable; the restricted `T & Any` form denotes definitely non-nullable types.        | No general source-level <code>A &#124; B</code>; sealed hierarchies model alternatives. See [Kotlin types][kotlin-types]. |
| TypeScript | `A & B`                                                                                                                | <code>A &#124; B</code>. This is the closest familiar spelling precedent; see [unions and intersections][ts-algebra].     |
| Rust       | `T: A + B` combines trait bounds; it is not an arbitrary intersection of value types. See [trait bounds][rust-bounds]. | An `enum` declares explicit variants, rather than an untagged <code>A &#124; B</code>. See [enumerations][rust-enums].    |
| Go         | Interface elements intersect type sets.                                                                                | <code>A &#124; B</code> joins terms in constraints, not general value types. See [interface types][go-spec].              |
| CDuce      | `A & B`; binds more tightly than union.                                                                                | <code>A &#124; B</code>; both operators are set-theoretic. See [types and patterns][cduce-types].                         |
| Ballerina  | `A & B`, with restrictions on intersection descriptors; not every arbitrary intersection is expressible.               | <code>A &#124; B</code>. See the [specification][ballerina-spec].                                                         |

### D4 — Relative difference, without unary negation

**Decision.** `A &! B` contains exactly the values of `A` that are not in `B`.
`&!` is one binary infix operator, not `&` followed by an independently available
prefix `!`. There is no general unary type-negation spelling `!T` or `~T`.

```text
Number &! 0
Integer &! Negative
A &! (B | C)
```

**Why relative exclusion.** The left operand makes the relevant universe explicit.
The complement of the singleton `true` would include numbers, strings and every
other non-`true` value, rather than just `false`. `Bool &! true` states the
intended domain. Likewise, excluding child-shaped records from `Person` is a
different operation from excluding them from the whole value universe.
The same concern applies to diagnostics: a relative exclusion preserves the
domain being narrowed instead of presenting an unexplained global complement.

If a top type is exposed, its complement of `B` is expressible as `Top &! B`.
Without a surface top type, unrestricted complement deliberately has no direct
spelling. This choice does not restrict the use of negation inside the solver.

**Why `&!`.** It reads as “and not”, relates visually to intersection and uses
ASCII. `\` is a serious mathematical alternative, but is less familiar in the
target language family and evokes escaping. This is an ergonomic objection,
not a claim that lexers cannot distinguish its roles. `-` is avoided to leave
room for future arithmetic on numeric types. `&~`, `&^` and word operators are
useful precedents, but `!` supplies the familiar “not” mnemonic.

**Still open.** The precedence and associativity of `&!` are not decided.
Difference is not associative: removing `B` and then `C` differs from removing
`B &! C`. Examples mixing it with other operators should show grouping explicitly
until the complete table is agreed. The chosen token requires `&!` to be
recognised ahead of the shorter `&`; this does not settle the rest of the lexer.

**Other languages.**

| Language   | Exclusion or nearest equivalent                                                                                                                                                        |
| ---------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Kotlin     | `!is` narrows control flow; no general denotable type difference. See [type checks][kotlin-checks].                                                                                    |
| TypeScript | `Exclude<T, U>` removes union members assignable to `U`; it is not arbitrary set subtraction. In particular, `Exclude<number, 0>` remains `number`. See [utility types][ts-utilities]. |
| Rust       | No general type subtraction; enum matching handles remaining alternatives. Value-level set subtraction is a different operation. See [types][rust-types] and [enums][rust-enums].      |
| Go         | No general type difference. Binary `&^` is bit clear (“AND NOT”), a syntactic precedent only. See [operators][go-spec].                                                                |
| CDuce      | `A \ B`; no separate unary complement operator in the documented Boolean type syntax. `Any \ B` expresses global complement. See [types and patterns][cduce-types].                    |
| Elixir     | Developing set-theoretic notation uses `A and not B` and `not B`. This differs from the older Erlang-style typespec language. See the [type cheatsheet][elixir-types].                 |
| Scala      | Value-level set difference `&~` is another “and not” spelling precedent, rather than a type-difference operator. See [set operations][scala-sets].                                     |

## Symbol table: syntax to semantics

This is the reverse lookup for symbol allocation, including context and unresolved
collisions. A provisional row does not reserve final grammar. Update it whenever
a decision changes a token's meaning or availability.

| Symbol / token           | Meaning                          | Context / role                                | Status / constraints                                                                        |
| ------------------------ | -------------------------------- | --------------------------------------------- | ------------------------------------------------------------------------------------------- |
| `&`                      | Intersection / AllOf             | Type expression, binary infix                 | Fixed; tighter than <code>&#124;</code>; associative chains                                 |
| <code>&#124;</code>      | Union / AnyOf                    | Type expression, binary infix                 | Fixed; associative chains                                                                   |
| `&!`                     | Relative difference              | Type expression, single binary infix operator | Fixed spelling; precedence and associativity open                                           |
| `(`, `)`                 | Group type operations            | Type expressions                              | Fixed for grouping; other uses open                                                         |
| Decimal digits           | Decimal significand or exponent  | Numeric literal                               | Fixed; significand requires at least one digit                                              |
| `.`                      | Decimal point                    | Numeric significand                           | Fixed; at most once; leading/trailing allowed; member-access and range boundaries open (D1) |
| `E`                      | Power-of-ten exponent marker     | Numeric literal                               | Fixed; uppercase; followed by integer exponent                                              |
| `_`                      | Ignored numeric separator        | Inside numeric literal                        | Fixed; no general wildcard meaning assigned                                                 |
| U+0020 SPACE             | Ignored numeric separator        | Inside numeric literal                        | Fixed; other whitespace excluded; collection and exponent/identifier boundaries open (D1)   |
| `+`, `-`                 | Optional exponent sign           | After numeric `E`                             | Fixed there; leading-sign and arithmetic syntax open                                        |
| The value literal itself | Singleton containing that value  | Type position                                 | Fixed contextual interpretation; no additional marker                                       |
| `!T`, `~T`               | General unary type complement    | Type expression                               | Deliberately unavailable; not implied by `&!` or `!...`                                     |
| `{ … }`, `:`, `,`        | Map/record structure and entries | Composite examples                            | Provisional; complete entry and delimiter grammar open                                      |
| `...`                    | Allow additional entries         | Map/record type remainder                     | Candidate; defaults open                                                                    |
| `!...`                   | Forbid additional entries        | Map/record type remainder                     | Candidate local marker, not general unary negation                                          |
| `...: T`                 | Constrain additional entries     | Map/record type remainder                     | Candidate; key/value-region semantics open                                                  |
| `?`                      | Optional entry or nullability    | Potential sugar                               | Unassigned; absence and null must be distinguished                                          |
| `[ … ]`, `< … >`         | Collections or parameters        | Illustrative examples                         | Unassigned; not an agreed grammar                                                           |

## Open design areas

### O1 — Maps, records, lists and bags

**Status: provisional candidates, no complete composite syntax decision.**
Finite maps are being explored as a common basis for composites. This could let
records, integer-indexed lists and multiplicity-based bags share semantics while
receiving readable surface forms. Both the derivation and its concrete notation
remain open.

For maps, distinguish three independent questions: which keys are permitted,
which keys must exist, and what values they may map to. An optional key is not
the same as a required key whose value may be null. Likewise, allowing additional
keys is different from making a named key optional.

Candidate remainder markers form a small family:

```text
{ a: A, ... }
{ a: A, !... }
{ a: A, ...: T }
```

The appeal is locality: openness or closedness is stated where the remaining
entries would appear. `!...` costs four characters, but retains the familiar
ellipsis and avoids assigning a general postfix operator. Alternatives included
`{| … |}` and postfix `!`. No default for omitted remainder markers, required/
optional arrow spelling, or full meaning of a restricted remainder was settled.
A local `!...` would be compatible with the decision against general `!T`.

| Language   | Use case                                      | Convention                                                                                                                                                                                                                |
| ---------- | --------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Kotlin     | Map and key access                            | `mapOf(k to v)`, `Map<K, V>`, `m[k]`                                                                                                                                                                                      |
| Kotlin     | Named records                                 | Data class and constructor                                                                                                                                                                                                |
| Kotlin     | Optional fields and extra keys                | Nullable members differ from map-key absence; no structural row marker                                                                                                                                                    |
| Kotlin     | Ordered collections and length                | `listOf(...)`, `List<T>`; size is a value                                                                                                                                                                                 |
| Kotlin     | Bags / multiplicities                         | Count map, e.g. `Map<T, Int>`                                                                                                                                                                                             |
| TypeScript | Map and key access                            | `Map<K, V>` or index-signature object; distinct key models                                                                                                                                                                |
| TypeScript | Named records                                 | `{ a: A }` / `{ a: value }`                                                                                                                                                                                               |
| TypeScript | Optional fields and extra keys                | `a?: A`; index signatures allow other keys; excess-property checks do not make a general exact type                                                                                                                       |
| TypeScript | Ordered collections and length                | `[a, b]`; `T[]` versus fixed tuple `[A, B]`                                                                                                                                                                               |
| TypeScript | Bags / multiplicities                         | Count map, e.g. `Map<T, number>`                                                                                                                                                                                          |
| Rust       | Map and key access                            | `HashMap<K, V>`; lookup returns `Option`                                                                                                                                                                                  |
| Rust       | Named records                                 | `struct S { a: A }` / `S { a: value }`                                                                                                                                                                                    |
| Rust       | Optional fields and extra keys                | Fixed struct fields; `Option<A>` is a present field, not an omitted key                                                                                                                                                   |
| Rust       | Ordered collections and length                | `vec![...]`, `Vec<T>`; array `[T; N]` carries length                                                                                                                                                                      |
| Rust       | Bags / multiplicities                         | Count map, e.g. `HashMap<T, usize>`                                                                                                                                                                                       |
| Go         | Map and key access                            | `map[K]V`; `v, ok := m[k]` distinguishes absence                                                                                                                                                                          |
| Go         | Named records                                 | `struct { A T }` / composite literal                                                                                                                                                                                      |
| Go         | Optional fields and extra keys                | Fixed struct fields; map membership is separate                                                                                                                                                                           |
| Go         | Ordered collections and length                | `[]T` slice; `[N]T` array                                                                                                                                                                                                 |
| Go         | Bags / multiplicities                         | Count map, e.g. `map[T]int`                                                                                                                                                                                               |
| CDuce      | Named records; optional fields and extra keys | Closed records enumerate fields; `..` allows additional fields and `=?` marks optional fields. See [types and patterns][cduce-types].                                                                                     |
| CDuce      | Ordered collections and length                | Sequence types use regular expressions, distinguishing one element of type T from arbitrary repetitions of T. See [types and patterns][cduce-types].                                                                      |
| Ballerina  | Named records; optional fields and extra keys | Inclusive `record { … }` and exclusive <code>record {&#124; … &#124;}</code> descriptors, optional fields and typed rest descriptors separate shape closure from field presence. See the [specification][ballerina-spec]. |

Sources: Kotlin [collection construction][kotlin-collections] and
[collection types][kotlin-collection-types]; TypeScript [object and tuple types][ts-objects]
and [Map][ts-map]; Rust [structs][rust-structs], [arrays][rust-arrays] and
[HashMap][rust-map]; Go [composite types and literals][go-spec]. Count maps are
encodings of bags, not claims of built-in multiset syntax.

These comparisons motivate checks for the eventual design: specify empty
composites, nesting, key access, required versus optional entries, residual
regions, list ordering and length, bag multiplicity, and whether structural sugar
denotes exactly the corresponding map type.

### O2 — Type-directed literal parsing

**Status: explored architecture, not adopted syntax or a parser contract.**
The proposed benefit is natural domain literals such as `30s`, `2001:db8::1` or
`#ff8040`, interpreted using an expected type rather than one globally disjoint
literal grammar. The sketch separates outer structure from leaf interpretation:

```text
source → raw structure with scalar source spans
       → resolve expected types
       → interpret scalar spans and validate resulting values
```

If the outer parser runs first, it must determine every scalar boundary without
consulting its eventual type-specific parser. Commas, braces, comments and
whitespace therefore still need a global contract. For example, a comment rule
must not discard `#ff8040` before a colour parser sees it. A quoted or explicitly
bounded escape form could support contents that conflict with the normal
boundaries; neither its delimiter nor a block form has been chosen.

Type-directed parsing would need to resolve the following questions:

| Case                            | Question or proposed direction                                                                                                                |
| ------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| No expected type                | Require an annotation or explicit interpretation, or define a separate inference rule; no choice yet                                          |
| Union <code>A &#124; B</code>   | Combining parser candidates must not make the semantic union depend on operand order; distinct successful values could require disambiguation |
| Intersection / refinement       | Filter candidate values by membership or predicates, rather than requiring every constraint to recognise the same lexical spelling            |
| Difference `A &! B`             | A possible model parses through `A`, then excludes results in `B`                                                                             |
| Map / list / record constructor | Compose interpretations for keys, elements and fields using expected component types                                                          |
| Alias                           | Decide how parser availability behaves for equivalent types and aliases                                                                       |
| Top or global complement        | A value set alone does not supply a parser for every possible representation                                                                  |
| Schema change                   | The same raw text may acquire another meaning; explicit interpretation and diagnostics need design                                            |

One candidate model is a relation between raw syntax and values, allowing zero,
one or several candidates before validation. That is a useful way to expose
ambiguity, not an agreed elaboration algorithm. A key unresolved distinction is
between a type as a set of values and representation metadata: two extensionally
equal types must not accidentally acquire different semantics because their
parser registrations differ. Tooling must also preserve source spans and obtain
type information before formatting or diagnosing the inside of such literals.

| Language   | Comparable mechanism and its limit                                                                                                                                                                                                                        |
| ---------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Kotlin     | Expected types influence numeric typing; domain parsing is normally an explicit function over a string, not arbitrary type-selected token syntax. See [literals][kotlin-literals].                                                                        |
| TypeScript | Contextual typing checks expressions; assertions are erased and do not parse strings into domain values. See [type assertions][ts-everyday].                                                                                                              |
| Rust       | `"…".parse::<T>()` selects `FromStr`; the host string boundary is already known. See [FromStr][rust-fromstr].                                                                                                                                             |
| Go         | Explicit functions such as `time.ParseDuration("30s")` parse domain text within ordinary strings. See [time][go-time].                                                                                                                                    |
| Wyvern     | Type-specific languages select a literal language through its expected type, including clearly delimited embedded content. A direct research precedent; see the [project description][wyvern].                                                            |
| YAML       | Restricted plain scalars, quoted/block alternatives and later tag resolution separate outer boundaries from interpretation. See the [specification][yaml-spec].                                                                                           |
| XSD        | Separates lexical representations from value spaces and composes list and union datatypes. Its ordered union-member interpretation is a caution: a first-match rule would conflict with order-independent set union here. See [datatypes][xsd-datatypes]. |

### O3 — Remaining syntax inventory

The following grouped backlog lists the remaining syntax requirements without
allocating spellings prematurely. Rows are open unless they refer to a decision above.
The comparisons show the nearest mechanism, including explicit absences.

| Area and question for this language                                                                                         | Kotlin                                                | TypeScript                                                                       | Rust                                                                                       | Go                                                                |
| --------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------- | -------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------ | ----------------------------------------------------------------- |
| Lexical foundations: names, qualification, keywords, comments, escaping, line breaks and separators                         | `pkg.Name`, backtick identifiers, `//`, `/* … */`     | Qualified names, `//`, `/* … */`; string property keys                           | Paths `a::B`, raw identifiers `r#name`, line/block comments                                | Qualified `pkg.Name`, line/block comments                         |
| Other primitives: Boolean/string escapes, tags, empty values; decide whether null/unit/absence exist                        | `true`, `"…"`, `null`, `Unit`, enums                  | `true`, `"…"`, `null`, `undefined`, `void`; literal unions                       | `true`, `"…"`, `()`, enum variants, `Option<T>`                                            | `true`, `"…"`, raw strings, `nil`; typed constants                |
| Elementary types: primitive names, aliases, top/bottom, nominal identity                                                    | `typealias`; `Any?`, `Nothing`; classes               | `type`; `unknown`, `never`; structural types                                     | `type`; structs/enums; `!` never type                                                      | Alias versus defined type; `any`; no general bottom               |
| Refinements: intervals, string/length/cardinality constraints, cross-field relations, quantification and static expressions | Predicates and validated wrappers; ranges are values  | Literal unions, conditional/template types and guards; no general predicate type | Validated newtypes; const parameters for some static constraints                           | Validation functions and defined types; no general predicate type |
| Type use: ascription, membership, subtype/equality notation, conversion and narrowing                                       | `val x: T`, `is`, `as`; smart casts                   | `x: T`, guards, `as`, `satisfies`; no general runtime test for arbitrary types   | `let x: T`, `match`, `as` / conversion traits; no general runtime type-membership operator | `var x T`, assertions, type switches, conversions                 |
| Reuse: definitions, aliases, generic/value parameters, recursion, visibility                                                | `typealias`, `<T>`, bounds; no general const generics | `type`, `<T>`, recursive aliases; literal type arguments are types               | `type`, `<T, const N: usize>`; recursive data through indirection                          | `type`, `[T Constraint]`; recursive-type restrictions             |
| Sugar and grammar: optionality, desugaring, precedence and token conflicts                                                  | `T?`, `?:`; fixed host grammar                        | <code>T &#124; null</code>, `a?: T`; fixed host grammar                          | `Option<T>`, macros; fixed host grammar                                                    | Pointers/interfaces for nil-capable values; fixed host grammar    |

Sources for this inventory: Kotlin [basic syntax][kotlin-basics],
[type system][kotlin-types], [generics][kotlin-generics] and [checks][kotlin-checks];
TypeScript [everyday types][ts-everyday], [generics][ts-generics],
[narrowing][ts-narrowing] and [utility types][ts-utilities]; Rust
[type catalogue][rust-types], [structs][rust-structs], [enums][rust-enums] and
[generic parameters][rust-generics]; Go [language specification][go-spec] and
[generics tutorial][go-generics]. These are comparisons of language mechanisms;
none supplies all the proposed refinement features.

A useful next decision order is outer literal boundaries and separators, then
maps and their required/optional/rest constraints, then collection sugar and
refinements, then declarations and reuse. Complete the operator table once its
participants are known. The current log does not choose this future syntax.

## Maintenance

For subsequent decisions, retain the stable D/O identifiers, add the semantics,
examples, rationale and serious alternatives in the relevant subsection, and
update the overview, symbol table and table of contents. Mark superseded choices
explicitly. Keep the rationale self-contained; link any supporting derivation
that maintainers need to follow rather than relying on unreferenced discussions.

[kotlin-literals]: https://kotlinlang.org/spec/expressions.html
[kotlin-types]: https://kotlinlang.org/spec/type-system.html
[kotlin-checks]: https://kotlinlang.org/docs/typecasts.html
[kotlin-collections]: https://kotlinlang.org/docs/constructing-collections.html
[kotlin-collection-types]: https://kotlinlang.org/docs/collections-overview.html
[kotlin-basics]: https://kotlinlang.org/docs/basic-syntax.html
[kotlin-generics]: https://kotlinlang.org/docs/generics.html
[ecma-lexical]: https://tc39.es/ecma262/multipage/ecmascript-language-lexical-grammar.html
[ts-separators]: https://www.typescriptlang.org/docs/handbook/release-notes/typescript-2-7.html
[ts-everyday]: https://www.typescriptlang.org/docs/handbook/2/everyday-types.html
[ts-algebra]: https://www.typescriptlang.org/docs/handbook/unions-and-intersections.html
[ts-utilities]: https://www.typescriptlang.org/docs/handbook/utility-types.html
[ts-objects]: https://www.typescriptlang.org/docs/handbook/2/objects.html
[ts-map]: https://github.com/microsoft/TypeScript/blob/v5.9.3/src/lib/es2015.collection.d.ts
[ts-generics]: https://www.typescriptlang.org/docs/handbook/2/generics.html
[ts-narrowing]: https://www.typescriptlang.org/docs/handbook/2/narrowing.html
[rust-tokens]: https://doc.rust-lang.org/reference/tokens.html
[rust-types]: https://doc.rust-lang.org/reference/types.html
[rust-bounds]: https://doc.rust-lang.org/reference/trait-bounds.html
[rust-enums]: https://doc.rust-lang.org/reference/items/enumerations.html
[rust-structs]: https://doc.rust-lang.org/reference/items/structs.html
[rust-generics]: https://doc.rust-lang.org/reference/items/generics.html
[rust-arrays]: https://doc.rust-lang.org/reference/types/array.html
[rust-map]: https://doc.rust-lang.org/std/collections/struct.HashMap.html
[rust-fromstr]: https://doc.rust-lang.org/std/str/trait.FromStr.html
[go-spec]: https://go.dev/ref/spec
[go-generics]: https://go.dev/doc/tutorial/generics
[go-time]: https://pkg.go.dev/time#ParseDuration
[cduce-types]: https://www.cduce.org/manual_types_patterns.html
[ballerina-spec]: https://ballerina.io/spec/lang/2024R1/
[elixir-types]: https://elixir.hexdocs.pm/main/types-cheat.html
[scala-sets]: https://www.scala-lang.org/api/current/scala/collection/SetOps.html
[wyvern]: https://www.cs.cmu.edu/~aldrich/wyvern/
[yaml-spec]: https://yaml.org/spec/1.2.2/
[xsd-datatypes]: https://www.w3.org/TR/xmlschema11-2/
