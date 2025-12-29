/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.rule.match.instructions;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.prover.rules.matcher.vm.instruction.MatchInstruction;
import org.key_project.rusty.logic.sort.ParametricSortInstance;
import org.key_project.rusty.logic.sort.SortArg;

import org.jspecify.annotations.Nullable;

public class SimilarParametricSortInstruction implements MatchInstruction {
    private final ParametricSortInstance psi;

    public SimilarParametricSortInstruction(ParametricSortInstance psi) {
        this.psi = psi;
    }

    @Override
    public @Nullable MatchResultInfo match(SyntaxElement actualElement,
            MatchResultInfo matchConditions, LogicServices services) {
        if (actualElement instanceof SortArg sa && sa.sort() instanceof ParametricSortInstance psi
                && psi.getBase() == this.psi.getBase()) {
            return matchConditions;
        }
        return null;
    }
}
