#![feature(stmt_expr_attributes)]
#![feature(proc_macro_hygiene)]

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
// An GHOSTHOUSE exmaple for the RQ3
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

pub fn room_as_usize(room: Room) -> usize {
    room as usize
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
    let entrance_idx = Room::Entrance as usize;
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

    let loc_idx: usize = gh.visitor_location as usize;
    let check_occupancy = gh.occupancy[entrance_idx];
    let check_panic = gh.visitor.close_to_panic;

    ghost!{
        proof_assert!(loc_idx == entrance_idx);
        proof_assert!(check_occupancy);
        proof_assert!(check_panic <= 100);
    };

    Ok(gh)
}
pub fn move_to(gh: &mut GhostHouse, target: Room) -> Result<(), HouseError> {
    if gh.visitor.aborted {
        return Err(HouseError::VisitorAborted);
    }

    let t = target.room_as_usize();
    let current_loc = gh.visitor_location;
    let mut flags = gh.occupancy;
    let current = gh.visitor_location.room_as_usize();

    if flags[t] {
        return Err(HouseError::RoomOccupied);
    }

    let old_flags = ghost!{snapshot!(flags)};
    let old_loc = ghost!{snapshot!(current_loc)};
    let old_loc_idx: usize = old_loc as usize;

    ghost!{
        proof_assert!((*old_flags)[old_loc_idx]);
        proof_assert!((*old_flags)[t]);
        proof_assert!(old_loc_idx != t);
    };

    flags[current] = false;
    flags[t] = true;
    let new_loc = target;

    let new_flags = ghost!{snapshot!(flags)};
    let new_loc_s = ghost!{snapshot!(new_loc)};

    let new_loc_idx: usize = new_loc_s as usize;

    ghost!{
        proof_assert!(new_loc_idx == t);
        proof_assert!((*new_flags)[old_loc_idx] == false);
        proof_assert!((*new_flags)[t]);
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
        proof_assert!( old_aborted == false);
        proof_assert!(old_panic <= 100);
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
        proof_assert!(new_panic >= old_panic);
        proof_assert!(new_panic <= 100);
        proof_assert!(!old_aborted || new_aborted);
    };

    gh.visitor.close_to_panic = panic;
    gh.visitor.aborted = aborted;

    Ok(())
}



