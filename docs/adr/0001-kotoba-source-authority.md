# ADR 0001: Kotoba is the ABA catalog source authority

- Status: Accepted
- Date: 2026-07-21

`src/association_facts.kotoba` is the sole production source. It preserves the
year-only `1985` restructuring and `1997` mission refocus without inventing a
founding date or finer precision. Unknown values and indexes fail closed; no
effects are declared. Conformance is semantic across reference, restricted
JavaScript, and instantiated typed WebAssembly; compiler-output byte identity
is not a gate. Clojure and the JVM are compiler/test hosts only.
