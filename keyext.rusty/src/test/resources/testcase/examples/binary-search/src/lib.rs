#![feature(stmt_expr_attributes)]
#![feature(proc_macro_hygiene)]

use rml_contracts::*;

#[spec {
    requires(forall(|x: usize, y: usize| x < y && y < N ==> arr[x] <= arr[y])),
    ensures(if exists(|x: usize| x < N && arr[x] == v) { arr[result.unwrap()] == v } else { result == None })
}]
#[pure]
pub fn binary_search_array<const N: usize>(arr: &[i128; N], v: i128) -> Option<usize> {
    let mut l = 0;
    let mut r = N - 1;

    if N == 0 {
        return None;
    }
    if N == 1 {
        return if arr[0] == v { Some(0) } else { None };
    }

    #[invariant(
        l < r && r < N && forall(|x: usize| x < l ==> arr[x] < v) && forall(|x: usize| r < x && x < N ==> v < arr[x])
    )]
    #[variant(r - l)]
    while r > l + 1 {
        let mid = l + (r - l) / 2;
        if arr[mid] == v {
            return Some(mid);
        }
        if arr[mid] > v { r = mid } else { l = mid }
    }

    if arr[l] == v {
        Some(l)
    } else if arr[r] == v {
        Some(r)
    } else {
        None
    }
}
