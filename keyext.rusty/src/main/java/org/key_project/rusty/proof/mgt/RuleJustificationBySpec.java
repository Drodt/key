/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.proof.mgt;

import org.key_project.rusty.speclang.Contract;

public record RuleJustificationBySpec(Contract spec) implements RuleJustification {
    public boolean isAxiomJustification() {
        // TODO
        return false;
    }
}
