#![feature(stmt_expr_attributes)]
#![feature(proc_macro_hygiene)]

use rml_contracts::*;

#[spec {
    ensures(true)
}]
pub fn increase_lower(a: &mut u64, b: &mut u64, amount: u64) {
    let lower = if a < b { a } else { b };
    *lower = *lower + amount;
}

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
