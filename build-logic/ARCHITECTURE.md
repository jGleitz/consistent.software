# Build Logic Architecture

This document is for maintainers of the Gradle build.
It collects the principles we follow when we change the build, each with the reason behind it and the change that applied it.

## Principles

### Declare Inputs and Outputs Precisely

A task's inputs and outputs must cover exactly the files the task reads and writes.
Never let them reach into directories that other tasks write while the task runs: `build/` directories, temporary directories such as VitePress's `.vitepress/.temp`, and symbolic links that Gradle follows, such as the workspace links pnpm hoists into `node_modules/.pnpm/node_modules`.
Gradle snapshots those files while the other task creates and deletes them, and the build fails with a `NoSuchFileException` at random ([#170]).

### Let Every Task Go Up to Date

Every task should be able to report `UP-TO-DATE`, verification tasks included.
A task that declares no outputs runs on every build.
If a task produces nothing, declare a marker file that it writes on success ([#175]).
If you consciously leave an input undeclared, document the gap and why you accept it next to the declaration.

### Only Cache Correct Results

A cached result is replayed without running the task, so it must be exactly what a fresh run would produce.
A test task must fail when its tests fail.
If it succeeds with `ignoreFailures` instead, Gradle caches it as green and later builds pass without running the failing tests ([#176]).
Before you make a task cacheable, declare every input that affects its result, including the versions of runtimes such as Node.js or the browser.

### Keep Configuration Independent of the Environment

Don't read volatile environment, such as `PATH`, while Gradle configures the build, not even indirectly through a library.
Gradle records it as a configuration input, so every change discards the configuration cache ([#173]).
The configuration cache report lists what was read and by whom.

### Measure Before Optimising

Find out where the time actually goes before you change anything: use `--profile`, the task timings, and the step timings of CI runs to find the critical path.
Then fix the cause instead of trimming constant factors.
For example, the Node.js tests were slow because every expected exception formatted a stack trace hundreds of frames deep, so we capped the stack depth ([#177]).
Expected exceptions in property tests stay cheap only as long as that cap is in place.

### Prefer the Tools' Own Settings

Use the options a tool maintains, such as log levels, over custom build code that achieves the same.
A successful task prints only warnings and errors; a failing task prints what is needed to understand the failure ([#172]).

### Fix Warnings at Their Source

Fix a warning where it originates instead of suppressing it.
If it comes from a third-party tool, leave it visible and document it with a link to the upstream issue ([#169]).

[#169]: https://github.com/jGleitz/consistent.software/pull/169
[#170]: https://github.com/jGleitz/consistent.software/pull/170
[#172]: https://github.com/jGleitz/consistent.software/pull/172
[#173]: https://github.com/jGleitz/consistent.software/pull/173
[#175]: https://github.com/jGleitz/consistent.software/pull/175
[#176]: https://github.com/jGleitz/consistent.software/pull/176
[#177]: https://github.com/jGleitz/consistent.software/pull/177
