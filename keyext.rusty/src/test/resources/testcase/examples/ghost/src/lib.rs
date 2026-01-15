#![feature(stmt_expr_attributes)]
#![feature(proc_macro_hygiene)]

extern crate rml_contracts;
use rml_contracts::*;
/*
#[spec {ensures(result == 2)}]
fn foo(a:i32) -> i32  {
    let old_a = ghost!{snapshot!(a)};
    let mut old_b = 1;
    ghost!{
        let old_a_a = &old_a;

        };
    let x: i32 = 2;
    old_b = old_b+1;
    x
}
#[spec {ensures(result == 2)}]
fn foo(a: &mut i32) -> i32  {
    let tmp: i32 = *a;
    let old_a = ghost!{snapshot!(tmp)};
    let one: Ghost<i32> = ghost!{ 1 };

     let now: i32 = tmp + 1;
     let check = true;

    ghost!{
        let old = &old_a;
        check == (old + &one);
        };

2
}*/

#[spec {ensures(result == 2)}]
pub fn foo(a: &mut [i32; 10])-> i32{
    //let tmp: i32 = *a;

    let old_a = ghost!{snapshot!(a)};
    let mut i: usize = 0;
    let len = 10;
    let b = a;

    loop{
        if i >= len {
            break;
        }
        b[i] = b[i] * 2;

        ghost!{
            let all_doubled = b[i] == (*old_a)[i]*2;
        };

        i +=1
    }
    2

}
/*//geht wieder
#[spec {
    requires(a <= 1000 && b <= 1000),
    ensures(result == a * b)
    }]
pub fn foo(a: u64, mut b: u64) -> u64 {
    let mut n: u64 = 0;
    let old_b: u64 = b;
    #[invariant(n == a * (old_b - b) && b <= old_b)]
    #[variant(b)]
    loop {
        if b == 0 { break n; }
        n += a;
        b -= 1;
    }
}*/
//Das Beispiel "test_array_enumerate" aus rustSrc/src/lib.rs
/*#[spec( ensures(result == 0))]
pub fn foo() -> i32 {
    let a = [1 + 2 - 3, 1, 1 + 1];
    a[0]
}*/