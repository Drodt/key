/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.prover.impl;

import org.key_project.prover.engine.GoalChooser;
import org.key_project.prover.engine.ProverCore;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.proof.init.Profile;

/// The central place where the prover for automatic proof search is selected (a "seam" in the
/// sense of Feathers: a place where behavior can be switched without editing the code that uses
/// it).
///
/// Returns the [ParallelProver] when the multicore prover is enabled _and_ the proof's
/// [`profile supports it`][Profile#supportsParallelAutomode()], and the standard single-threaded
/// [ApplyStrategy] otherwise. Construction sites that drive automode should go through here
/// instead of constructing [ApplyStrategy] directly, so the parallel path can be toggled
/// centrally and the profile guard cannot be bypassed.
public final class AutoProvers {
    private AutoProvers() {}

    /// Creates the auto-prover selected by the current configuration for the given profile.
    ///
    /// @param goalChooser the goal chooser to use
    /// @param profile the profile of the proof to be processed; the parallel prover is used only if
    /// it [`supports parallel automode`][Profile#supportsParallelAutomode()]
    /// @return a [ParallelProver] if enabled and supported, otherwise an [ApplyStrategy]
    public static ProverCore<Proof, Goal> create(GoalChooser<Proof, Goal> goalChooser,
            Profile profile) {
        return create(goalChooser, profile, true);
    }

    /// Creates the auto-prover selected by the current configuration, optionally forcing the
    /// single-threaded prover.
    ///
    /// @param goalChooser the goal chooser to use
    /// @param profile the profile of the proof to be processed
    /// @param allowParallel whether the parallel prover may be used at all for this run
    /// @return a [ParallelProver] if allowed, enabled and supported, otherwise an
    /// [ApplyStrategy]
    public static ProverCore<Proof, Goal> create(GoalChooser<Proof, Goal> goalChooser,
            Profile profile, boolean allowParallel) {
        boolean parallel =
            allowParallel && ParallelProver.isEnabled() && profile.supportsParallelAutomode();
        return parallel ? new ParallelProver(goalChooser) : new ApplyStrategy(goalChooser);
    }
}
