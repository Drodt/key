#![feature(stmt_expr_attributes)]
#![feature(proc_macro_hygiene)]
#![allow(clippy::all)]
#![allow(dead_code)]

extern crate rml_contracts;
use rml_contracts::*;

#[spec { name = "normal",
    requires(a + b <= i32::MAX),
    requires(a + b >= i32::MIN),
    ensures(result == a + b)
}]
fn add(a: i32, b: i32) -> i32 {
    let mut x = ghost! {1};
    ghost! {
        *x = 2;
    };
    a + 1
}

struct MyStruct {
    f1: Vec<i32>,
}

fn allowed() {
    // Copy type (i32): can be used directly
    let x: i32 = 5;
    let y = ghost! {
        x + 1;
    };

    //  Reference to non-Copy (String): allowed
    let s: String = String::from("abc");
    ghost! {
        (&s).len()
    };

    // Clone of non-Copy (String): explicitly allowed
    let s1: String = String::from("abc");
    let y1 = ghost! {
        s.clone().len()
    };

    //  Clone of Vec (non-Copy): explicitly allowed
    let vec = vec![1, 2, 3];
    ghost! {
        let ghost_vec = vec.clone();
        ghost_vec.len()
    };

    let ms = MyStruct { f1: vec![0] };
    let gms: Ghost<MyStruct> = ghost! {snapshot!(ms)};
}

fn not_allowed(){
    let s: String = String::from("hello");
    ghost!{
        s.len()
    };

    let v = vec![1,2,3];
    ghost!{
        v.len()
    };
}


fn main() {}
