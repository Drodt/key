# Casts in the logic + cast insertion during taclet application

## What

Adds the cast machinery legacy KeY uses to keep terms well-sorted, ported to the
parametric-function style (no sort-depending functions):

- **`cast.key`** — a parametric `cast<[E]>(any) -> E` function (first-order;
  counterpart of `ghost<[E]>`/`read<[E]>`) with its simplification taclets
  (`castDel`, nested-cast collapse), included after `firstOrderRules`;
  `cast_deletion` ruleset registered.
- **`TermBuilder.cast(Sort, Term)`** — builds `cast<[sort]>(t)` by instantiating
  the parametric `cast` at the target sort.
- **`RewriteTacletExecutor`** — computes the per-position required sort
  (`getMaxSort`, a `TermHelper` port) and, when a replacewith result doesn't fit,
  wraps it in a cast — restoring the previously stubbed/commented-out logic.

## Open question for maintainers ⚠️

Six `RunAllProofs` cases fail to **replay** (e.g. `man-loop-mul.proof`):

```
Could not apply applyEq — Unknown instantiations for \assumes. 0 candidates.
```

The cast insertion changes the term representation (loose terms now appear as
`cast<[…]>(…)`), so proofs saved under the *old* representation no longer match.
`cast.key` and `TermBuilder.cast` are inert; only the executor insertion changes
terms — which is its purpose.

**Please decide:**

1. **Adopt** the well-sorted representation → re-save the 6 affected proofs
   (`man-loop-mul.proof` and siblings under `testcase/examples/`). *(expected)*
2. **Too disruptive** → gate the executor cast insertion behind a flag, or keep
   only `cast.key` + `TermBuilder.cast` for explicit use by taclets.
