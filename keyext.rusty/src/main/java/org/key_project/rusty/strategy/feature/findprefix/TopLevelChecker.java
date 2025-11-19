/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.strategy.feature.findprefix;

import org.key_project.prover.sequent.PosInOccurrence;

/// Checks, whether the position in occurrence is top level.
///
/// @author christoph
public class TopLevelChecker implements Checker {
    @Override
    public boolean check(PosInOccurrence pio) {
        return pio.isTopLevel();
    }
}
