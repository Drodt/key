#![feature(stmt_expr_attributes)]
#![feature(proc_macro_hygiene)]

extern crate rml_contracts;
use rml_contracts::*;
/*
#[spec {ensures(result == 2)}]
pub fn foo(a: &mut [i32; 10])-> i32{

    let old_a = ghost!{snapshot!(a)};
    let mut i: usize = 0;
    let len = 10;
    let b = a;

    #[invariant(0 <= i && i <= len)]
    #[variant(len-i)]
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
}*/

pub struct Person {
    pub id: usize,
    pub close_to_panic: u8, // 0..=100
    pub aborted: bool,
}

pub enum Room {
    Entrance = 0,
    CreepyBasement = 1,
    Laboratory = 2,
    Kitchen = 3,
    TortureRoom = 4,
}

pub const ROOM_COUNT: usize = 5;

pub enum HouseError {
    RoomOccupied,
    VisitorAborted,
    InvalidPanicValue,
}

pub struct GhostHouse {
    visitor_location: Room,
    visitor: Person,
    occupancy_flags: [bool; ROOM_COUNT], // true = occupied
    room_fear: [u8; ROOM_COUNT],         // 0..=100 per room
}
#[spec {ensures(result == 2)}]
pub fn foo()-> i32{

    2}
