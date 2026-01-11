#![feature(stmt_expr_attributes)]
#![feature(proc_macro_hygiene)]

extern crate rml_contracts;
use rml_contracts::*;

#[spec {ensures(result == 2)}]
fn foo(a: [i32; 48]) -> i32  {
    let old_a = ghost!{snapshot!(a)};
    let x: i32 = 2;
    x
}
