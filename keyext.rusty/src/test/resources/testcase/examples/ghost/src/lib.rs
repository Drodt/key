#![feature(stmt_expr_attributes)]
#![feature(proc_macro_hygiene)]
#![allow(clippy::all)]
#![allow(dead_code)]

extern crate rml_contracts;
use rml_contracts::*;

#[spec { ensures(result == 2) }]
pub fn double_array_with_ghost(a: [i32; 10]) -> i32 {
    let old_a = ghost! { snapshot!(a) };

    let mut i: usize = 0;
    let len: usize = 10;
    let mut b = a;
    let mut v: usize = len;

    #[invariant(0 <= v && v <= len)]
    #[invariant(v == len - i)]
    #[variant(v)]
    loop {
        if i < len {
            b[i] = b[i] * 2;

            ghost! {
                let all_doubled = b[i] == old_a[i] * 2;
            };

            i += 1;
            v -= 1;
            continue;
        } else {
            break;
        }
    }

    2
}

#[spec { ensures(result == 2) }]
pub fn double_array_without_ghost(a: [i32; 10]) -> i32 {
    let old_a = a;

    let mut i: usize = 0;
    let len: usize = 10;
    let mut b = a;
    let mut v: usize = len;

    #[invariant(0 <= v && v <= len)]
    #[invariant(v == len - i)]
    #[variant(v)]
    loop {
        if i < len {
            b[i] = b[i] * 2;

            let all_doubled = b[i] == old_a[i] * 2;

            i += 1;
            v -= 1;
            continue;
        } else {
            break;
        }
    }

    2
}

#[spec {ensures(result == 2)}]
pub fn reverse_array_with_ghost(a: [i32; 10]) -> i32 {
    let mut b = a;

    let old_b = ghost! { snapshot!(b) };

    let mut i = 0;
    let len = 10;
    let mut v = len / 2;

    #[invariant(0 <= i && i <= len / 2)]
    #[invariant(0 <= v && v <= len / 2)]
    #[invariant(v == (len / 2) - i)]
    #[variant(v)]
    loop {
        if i < len / 2 {
            let j = (len - 1) - i;

            let tmp= b[i];
            b[i] = b[j];
            b[j] = tmp;

            ghost! {
                let left= b[i] == old_b[j];
                let right = b[j] == old_b[i];
                let check = left && right;
            };

            i += 1;
            v -= 1;
            continue;
        } else {
            break;
        }
    }

    ghost! {
        let proof = b[0] == old_b[len - 1] && b[len - 1] == old_b[0];
        let check = proof;
    };

    2
}

#[spec {ensures(result == 2)}]
pub fn reverse_array_without_ghost(a: [i32; 10]) -> i32 {
    let mut b = a;

    let old_b = a;

    let mut i = 0;
    let len = 10;
    let mut v = len / 2;

    #[invariant(0 <= i && i <= len / 2)]
    #[invariant(0 <= v && v <= len / 2)]
    #[invariant(v == (len / 2) - i)]
    #[variant(v)]
    loop {
        if i < len / 2 {
            let j = (len - 1) - i;

            let tmp= b[i];
            b[i] = b[j];
            b[j] = tmp;

            let left= b[i] == old_b[j];
            let right = b[j] == old_b[i];
            let check = left && right;


            i += 1;
            v -= 1;
            continue;
        } else {
            break;
        }
    }
    let proof = b[0] == old_b[len - 1] && b[len - 1] == old_b[0];
    let check = proof;

    2
}
#[spec {
    ensures(result > a && result > b)
}]
pub fn if_with_ghost(a: u32, b: u32) -> u32 {
    let old_a = a;
    let old_b = b;

    let res  =
        if a > b {
            let r = a + 1;

            ghost! {
                let test_cond = old_a > old_b;
                let test_a = r > old_a;
                let test_b = r > old_b;
                let test_final = test_cond && test_a && test_b;
            };

            r
        } else {
            let r = b + 2;

            ghost! {
                let test_cond = !(old_a > old_b);
                let test_b = r > old_b;
                let test_a = r > old_a;
                let test_final = test_cond && test_b && test_a;
            };

            r
        };

    res
}

#[spec {
    ensures(result > a && result > b)
}]
pub fn if_without_ghost(a: u32, b: u32) -> u32 {
    let old_a = a;
    let old_b = b;

    let res  =
        if a > b {
            let r = a + 1;

            let test_cond = old_a > old_b;
            let test_a = r > old_a;
            let test_b = r > old_b;
            let test_final = test_cond && test_a && test_b;

            r
        } else {
            let r = b + 2;

            let test_cond = !(old_a > old_b);
            let test_b = r > old_b;
            let test_a = r > old_a;
            let test_final = test_cond && test_b && test_a;

            r
        };

    res
}


/*
// An GHOSTHOUSE exmaple for the RQ3
pub struct Person {
    pub id: usize,
    pub close_to_panic: u8, // 0..=100
    pub aborted: bool,
}
#[derive(Copy, Clone)]
pub enum Room {
    Entrance,
    CreepyBasement,
    Laboratory,
    Kitchen,
    TortureRoom,
}


pub const ROOM_COUNT: usize = 5;

pub fn room_as_usize(room: Room) -> usize {
    match room {
        Room::Entrance => 0,
        Room::CreepyBasement => 1,
        Room::Laboratory => 2,
        Room::Kitchen => 3,
        Room::TortureRoom => 4,
    }
}

pub enum HouseError {
    RoomOccupied,
    VisitorAborted,
    InvalidPanicValue,
}

pub struct GhostHouse {
    visitor_location: Room,
    visitor: Person,
    occupancy: [bool; ROOM_COUNT], // true = occupied
    room_fear: [u8; ROOM_COUNT],         // 0..=100 per room
}

pub fn new_ghost_house(visitor: Person) -> Result<GhostHouse, HouseError> {
    if visitor.close_to_panic > 100 {
        return Err(HouseError::InvalidPanicValue);
    }
    let mut occ = [false; ROOM_COUNT];
    let entrance_idx = room_as_usize(Room::Entrance);
    occ[entrance_idx] = true;

    let room_fear: [u8; ROOM_COUNT] = [
        5,  // Entrance
        10, // CreepyBasement
        7,  // Laboratory
        3,  // Kitchen
        15, // TortureRoom
    ];

    let gh = GhostHouse{
        visitor_location: Room::Entrance,
        visitor,
        occupancy: occ,
        room_fear,
    };

    let loc_idx = room_as_usize(gh.visitor_location);
    let check_occupancy = gh.occupancy[entrance_idx];
    let check_panic = gh.visitor.close_to_panic;

    ghost!{
        let at_entrance = loc_idx == entrance_idx;
        //proof_assert!(at_entrance);

        let entrance_occ = check_occupancy == true;
        //proof_assert!(entrance_occ);

        let panic_range = check_panic <= 100;
        //proof_assert!(panic_range);

        let _checks = (at_entrance, entrance_occ, panic_range);
    };

    Ok(gh)
}
pub fn move_to(gh: &mut GhostHouse, target: Room) -> Result<(), HouseError> {
    if gh.visitor.aborted {
        return Err(HouseError::VisitorAborted);
    }

    let t = room_as_usize(target);
    let current_loc = gh.visitor_location;
    let mut flags = gh.occupancy;
    let current = room_as_usize(gh.visitor_location);

    if flags[t] {
        return Err(HouseError::RoomOccupied);
    }

    let old_flags = ghost!{snapshot!(flags)};
    let old_loc = ghost!{snapshot!(current_loc)};
    let old_loc_idx = room_as_usize(current_loc);

    ghost!{
        let pre_current_occupied = (*old_flags)[old_loc_idx] == true;
        //proof_assert!(pre_current_occupied);

        let pre_target_free = (*old_flags)[t] == false;
        //proof_assert!(pre_target_free);

        let idx_distinct = old_loc_idx != t;
        //proof_assert!(idx_distinct);

        let _checks1 = (pre_current_occupied, pre_target_free, idx_distinct);
    };

    flags[current] = false;
    flags[t] = true;
    let new_loc = target;

    let new_flags = ghost!{snapshot!(flags)};
    let new_loc_s = ghost!{snapshot!(new_loc)};

    let new_loc_idx = room_as_usize(new_loc);

    ghost!{
        let post_loc_updated = new_loc_idx == t;
        //proof_assert!(post_loc_updated);

        let post_current_free = (*new_flags)[old_loc_idx] == false;
        //proof_assert!(post_current_free);

        let post_target_occ = (*new_flags)[t] == true;
        //proof_assert!(post_target_occ);

        let _checks2 = (post_loc_updated, post_current_free, post_target_occ);
    };

    gh.occupancy = flags;
    gh.visitor_location = new_loc;

    Ok(())
}

/// Ghost scares the visitor, increasing panic.
pub fn scare(gh: &mut GhostHouse, intensity: u8) -> Result<(), HouseError> {
    if gh.visitor.aborted {
        return Err(HouseError::VisitorAborted);
    }

    let mut panic = gh.visitor.close_to_panic;
    let mut aborted = gh.visitor.aborted;
    let old_panic = ghost! { snapshot!(panic) };
    let old_aborted = ghost! { snapshot!(aborted) };

    ghost! {
        let _pre_aborted = old_aborted == false;
        //proof_assert!(_pre_aborted);

        let _pre_range = old_panic <= 100;
        //proof_assert!(_pre_range);

        let _checks1 = (_pre_aborted, _pre_range);
    };

    let mut new_panic: u8;
    if (255 - panic) < intensity {
        new_panic = 255;
    } else {
        new_panic = panic + intensity;
    }

    if new_panic > 100 {
        new_panic = 100;
    }

    panic = new_panic;

    if panic >= 90 {
        aborted = true;
    }

    let new_panic = ghost! { snapshot!(panic) };
    let new_aborted = ghost! { snapshot!(aborted) };

    ghost! {
        let panic_not_decreased = new_panic >= old_panic;
        //proof_assert!(panic_not_decreased);

        let panic_bounded = new_panic <= 100;
        //proof_assert!(panic_bounded);

        let aborted_monotonic = old_aborted == true || new_aborted == true;
        //proof_assert!(aborted_monotonic);

        let _checks2 = (panic_not_decreased, panic_bounded, aborted_monotonic);
    };

    gh.visitor.close_to_panic = panic;
    gh.visitor.aborted = aborted;

    Ok(())
}


*/