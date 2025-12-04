#![feature(stmt_expr_attributes)]
#![feature(proc_macro_hygiene)]

use rml_contracts::*;

#[spec {
    requires(forall(|x: usize, y: usize| x < y && y < N ==> arr[x] <= arr[y])),
    ensures(result == exists(|x: usize| x < N && arr[x] == v))
}]
#[pure]
pub fn contains<const N: usize>(arr: &[i128; N], v: i128) -> bool {
    let mut l = 0;
    let mut r = N - 1;

    if N == 0 {
        return false;
    }
    if N == 1 {
        return arr[0] == v;
    }

    #[invariant(
        0 <= l && l < r && r < N && forall(|x: usize| x < l ==> arr[x] < v) && forall(|x: usize| r < x && x < N ==> v < arr[x])
    )]
    #[variant(r - l)]
    while r > l + 1 {
        let mid = l + (r - l) / 2;
        if arr[mid] == v {
            return true;
        }
        if arr[mid] > v { r = mid } else { l = mid }
    }

    if arr[l] == v {
        true
    } else if arr[r] == v {
        true
    } else {
        false
    }
}
