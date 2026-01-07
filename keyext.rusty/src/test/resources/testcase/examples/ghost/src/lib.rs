#![feature(stmt_expr_attributes)]
#![feature(proc_macro_hygiene)]

extern crate rml_contracts;
use rml_contracts::*;

#[spec(ensures(true))]
fn foo() {
    ghost!{
        let x = 1;
    };
}

#[spec(ensures(true))]
fn foo1() {
    let x = 0;
    ghost!{snapshot!(x)};
}