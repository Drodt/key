#![feature(stmt_expr_attributes)]
#![feature(proc_macro_hygiene)]

extern crate rml_contracts;
use rml_contracts::*;

#[spec(ensures(true))]
fn foo() {
    let x = 1;
    let y = ghost!{ x + 2 };
    let z = x - 1;
}