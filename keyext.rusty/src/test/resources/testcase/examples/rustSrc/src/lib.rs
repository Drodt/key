#![feature(stmt_expr_attributes)]
#![feature(proc_macro_hygiene)]

extern crate rml_contracts;
use rml_contracts::*;

#[spec { name = "add_no_bounds",
    ensures(result == a + b)
    }]
pub fn add(a: u32, b: u32) -> u32 {
    a + b
}

#[spec {
    ensures(result == 4)
    }]
pub fn mut_example(mut a: u32, mut b: u32) -> u32 {
    let mut x = &mut a;
    *x = 0;
    x = &mut b;
    *x = 4;
    let c = a + b;
    c
}

#[spec {
    ensures(result > a && result > b)
    }]
pub fn if_example(a: u32, b: u32) -> u32 {
    if a > b {
        a + 1
    } else {
        b + 2
    }
}

// First verified function
#[spec {
    requires(a <= 1000 && b <= 1000),
    ensures(result == a * b)
    }]
pub fn mul(a: u64, mut b: u64) -> u64 {
    let mut n: u64 = 0;
    let old_b: u64 = b;
    #[invariant(n == a * (old_b - b) && b <= old_b)]
    #[variant(b)]
    loop {
        if b == 0 { break n; }
        n += a;
        b -= 1;
    }
}

#[spec(name = "array", ensures(result == 2))]
pub fn test_array() -> i32 {
    let mut a = [1 + 2 - 3; 4];
    a[0] = 1;
    a[1] = a[0] + 1;
    a[1]
}

#[spec(name = "tuple", ensures(result === (1, 4)))]
pub fn test_tuple() -> (i32, i32) {
    let mut a = (1 + 1, 2 * 2);
    a.0 = a.1 - 3;
    a
}