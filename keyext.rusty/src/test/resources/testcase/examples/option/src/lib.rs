#![feature(stmt_expr_attributes)]
#![feature(proc_macro_hygiene)]

extern crate rml_contracts;
use rml_contracts::*;

#[spec(ensures(true))]
fn foo() -> Option<u32> {
    let x = Some(1u32);
    None
}