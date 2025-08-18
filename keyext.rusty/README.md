# Rusty KeY

This folder contains the KeY version for Rust, implementing RustyDL.

## Installation

### Installing Rust

To run Rusty KeY, one requires Rust, Cargo, and rustup. For details see [the Rust home page](https://www.rust-lang.org/).

For a quick install of rustup, which manages all Rust utilities, run

```sh
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
```

Our setup ensures that rustup always picks the correct version of Rust to run.
This means, that executing Rusty KeY may take some additional time when it is run for the first time,
as we install a new Rust version.

### Setting up Libraries

We depend on a wrapper of the Rust compiler. Before using Rusty KeY, run

```shell
git submodule update --init --recursive
```

in the `key` folder.

## Examples & Testing

Examples can be found in [`keyext.rusty/src/test/resources/testcase/examples/`](./src/test/resources/testcase/examples/).

To run all proofs in the example folder use

```shell
./gradlew :keyext.rusty:testRunAllProofs
```

## Feature Support

### Calculus Rules

#### Expressions

- Async block
  - [ ] Implementation
  - [ ] Tests
  - [ ] Soundness proof
- Block
  - [x] Implementation
  - [x] Tests
  - [ ] Soundness proof
- Const Block
  - [ ] Implementation
  - [ ] Tests
  - [ ] Soundness proof
- If
  - [x] Implementation
    - Are they complete?
  - [ ] Tests
    - Some, not all
  - [ ] Soundness proof
    - Some, not all
- IfLet
  - [ ] Implementation
  - [ ] Tests
  - [ ] Soundness proof
- Loop
  - [x] Implementation
  - [ ] Tests
    - [x] Test w/o labels
    - [ ] Test w/ labels
  - [ ] Soundness proof
- Match
  - [ ] Implementation
  - [ ] Tests
  - [ ] Soundness proof
  - Worked on by BSC student
- Unsafe Block
  - [ ] Implementation
  - [ ] Tests
  - [ ] Soundness proof
- Labeled Block
  - [ ] Implementation
  - [ ] Tests
  - [ ] Soundness proof
- Array Expression
  - [ ] Implementation
  - [ ] Tests
  - [ ] Soundness proof
- Await
  - [ ] Implementation
  - [ ] Tests
  - [ ] Soundness proof
- Break
  - [ ] Implementation
    - Partially
  - [ ] Tests
    - Partially
  - [ ] Soundness proof
- Call
  - [ ] Implementation
    - Partially
  - [ ] Tests
    - Partially
  - [ ] Soundness proof
- Closure
- Continue
- Field
- Index
  - [ ] Implementation
    - [x] For arrays
  - [ ] Tests
    - [x] For arrays
  - [ ] Soundness proof
- LiteralExpression
  - [ ] Implementation
    - [x] Integer
    - [x] Boolean
  - [ ] Tests
  - [ ] Soundness proof
- Method call
- Macro call
  - Handled by rustc
-
- Path expression
  - [ ] Implementation
    - [x] Simple paths
  - [ ] Tests
    - [x] Simple paths
  - [ ] Soundness proof
    - [x] Simple paths
- Return
  - [x] Implementation
  - [x] Tests
  - [ ] Soundness proof
- Struct
- Tuple
  - [ ] Implementation
    - [x] Unit
  - [ ] Tests
  - [ ] Soundness proof

#### Statements

- [x] Expression statement
  - Handled by the expression rules and `simple_expr_stmt`
- [x] Let
- [ ] Empty
- [ ] Item

### Types

- Primitive
  - Int
    - [ ] u8
    - [ ] u16
    - [ ] u32
    - [ ] u64
    - [ ] u128
    - [ ] usize
    - [ ] i8
    - [ ] i16
    - [ ] i32
    - [ ] i64
    - [ ] i128
    - [ ] isize
  - [x] `bool`
  - Float
    - [ ] f16
    - [ ] f32
    - [ ] f64
    - [ ] f128
  - [ ] Tuple
    - [x] Unit
  - [x] Array
  - [ ] char
  - [ ] Pointer
  - [ ] Ref
    - [x] Of PVs
  - [ ] Slice
  - [ ] str
  - [ ] never
- ADTs
  - [ ] Structs
  - [ ] Enums
  - [ ] Unions

### Functions

- [ ] Generic functions
- [ ] Higher order functions
