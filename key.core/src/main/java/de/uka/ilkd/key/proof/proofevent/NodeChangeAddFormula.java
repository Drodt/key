/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.proof.proofevent;

import org.key_project.prover.sequent.PosInOccurrence;


/**
 * Information about a formula that has been added to a node (the position given is the position of
 * the formula within the new sequent)
 */
public class NodeChangeAddFormula extends NodeChangeARFormula {
    public NodeChangeAddFormula(PosInOccurrence p_pos) {
        super(p_pos);
    }

    public String toString() {
        return "Formula added: " + getPos();
    }
}
