# AGENTS.md

## Repository overview

This is a multi-module Maven repository for `java-miniterm`, a small terminal-oriented Java library family.

### Shared conventions

- Use Java and Maven conventions already present in the repo.
- Keep changes minimal and module-scoped unless a root-level change is required.
- Prefer `./mvnw` over a system Maven install.
- Keep formatting compatible with Spotless and google-java-format (AOSP style).
- Root artifacts and release tooling live in `pom.xml` and `jreleaser.yml`.
- Example programs live in `examples/` and should stay aligned with the public APIs.

### Build and validation

- Root build: `./mvnw clean install`
- Formatting is enforced through Spotless during `verify`.
- Release packaging uses the Maven `release` profile.

## Root module

- `pom.xml` is the parent POM for the entire repo.
- It defines the shared dependency and plugin versions.
- It manages `jreleaser-maven-plugin` and `spotless-maven-plugin` for all modules.
- Keep module ordering in sync with the actual repository layout.

## `miniterm`

- Legacy implementation targeting Java 8.
- Keep compatibility with Java 8 APIs and language level.
- Native Windows support exists here; be careful with the `process-classes` native DLL build logic.
- Integration tests may run in-process on macOS to preserve TTY behavior.

## `miniterm-ffm`

- Modern implementation targeting Java 22+.
- Preserve FFM-native access behavior and `--enable-native-access=ALL-UNNAMED` runtime/test wiring.
- Keep the Java 22 compiler release and Java-version enforcement intact.
- This module has the same public terminal API shape as `miniterm`, but different internals.

## `ansiparser`

- Java 8+ ANSI escape parser.
- Keep the parser state machine small and allocation-conscious.
- It is used directly by `miniterm`, `miniterm-ffm`, `mousetrack`, and `termcap`.
- Preserve both stateless (`AnsiParser`) and stateful (`AnsiReader`) entry points.

## `mousetrack`

- Java 8+ mouse tracking helper library.
- Keep protocol enable/disable helpers and event parsing aligned with the documented wire formats.
- Preserve support for X10, SGR, and URXVT, plus SGR-Pixels where applicable.
- Always clean up terminal tracking state in examples and tests.

## `termcap`

- Java 8+ terminal capability detection library.
- Passive detection should remain side-effect free and not require terminal I/O.
- `TermProber` is optional and depends on `ansiparser` for live probing.
- Keep detection-layer precedence stable unless explicitly changing the contract.

## `colors`

- Java 8+ OSC palette querying and setting library.
- Query APIs expect raw mode and an `IntReader` that signals timeout/EOF with negative values.
- Keep the 256-entry palette helpers and reset/query sequences aligned with OSC semantics.
- `ansiparser` is a required transitive dependency here for OSC response parsing.

## Documentation and examples

- Keep root and module READMEs consistent with public APIs and Maven coordinates.
- Update example scripts and `examples/*.java` when public usage changes.
- Prefer concise docs that reflect the current module contracts rather than broad tutorials.
