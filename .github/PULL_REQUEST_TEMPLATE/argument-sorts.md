# Enforce function argument sorts at term construction

## What

`RFunction` now validates that every argument's sort conforms to the declared
argument sort (`validTopLevelException`), so ill-typed terms are rejected at
construction instead of being built silently. This mirrors legacy KeY's
sorted-operator check (the modular `ncore` refactor dropped it).

Escapes keep taclet construction and proof search working:
- schema-variable subterms (matched loosely; checked at instantiation),
- a **generic** declared argument sort (e.g. the `E` of a parametric
  `ghost<[E]>`) accepts any term — the generic is bound at apply time,
- the top sort `any`, the meta sort, and program-SV sorts.

## Open question for maintainers ⚠️

Six example proofs now fail to load because the check rejects:

```
arrPlace<[int, const 4]>(refM<[int]>([[x]]), 1)
```

`arrPlace<[M, const N: int]>(MRef<[Array<[M, const N]>]>, int)` expects its first
argument to be `MRef<[Array<[int, 4]>]>` (a ref to an **array**), but the example
passes `refM<[int]>([[x]])` = `MRef<[int]>` (a ref to the **scalar** `x: u32`).
These parametric-sort instances are genuinely different.

Affected: `fm26/example3.key` and 5 siblings.

This looks like the examples are **ill-typed** (ref to a scalar where a ref to an
array is required) — i.e. the check is correctly catching a real inconsistency.
**Please confirm:**

1. **Examples are wrong** → fix them (ref the array, not the scalar); this PR is
   correct as-is. *(preferred reading)*
2. **Examples are intended** → we add a looser escape for parametric-sort-instance
   arguments, which weakens the check.
