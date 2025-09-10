/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.proof.proofevent;

import org.key_project.prover.sequent.PosInOccurrence;

import org.jspecify.annotations.NonNull;

/// An instance of this class informs the listerns if a formula has been tried to add to the sequent
///
/// @param pio the PosInOccurrence of the formula that has been tried to add
public record NodeRedundantAddChange(PosInOccurrence pio) implements NodeChange {
    /// creates an instance
    ///
    /// @param pio the PosInOccurrence of the formula that has been tried to add
    public NodeRedundantAddChange {
    }

    /// returns the PosInOccurrence of the formula that has been tried to add
    ///
    /// @return the PosInOccurrrence
    @Override
    public PosInOccurrence getPos() {
        return pio;
    }

    /// toString
    @Override
    public @NonNull String toString() {
        return "Redundant formula:" + pio;
    }

}
