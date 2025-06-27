/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.rule.match.instructions;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.prover.rules.matcher.vm.instruction.MatchInstruction;
import org.key_project.rusty.logic.op.SortDependingFunction;

import org.jspecify.annotations.Nullable;

public class SimilarSortDependingFunctionInstruction implements MatchInstruction {
    private final SortDependingFunction sortDependingFunction;

    public SimilarSortDependingFunctionInstruction(SortDependingFunction sortDependingFunction) {
        this.sortDependingFunction = sortDependingFunction;
    }

    @Override
    public @Nullable MatchResultInfo match(SyntaxElement actualElement,
            MatchResultInfo matchConditions, LogicServices services) {
        if (((SortDependingFunction) actualElement).isSimilar(sortDependingFunction)) {
            return matchConditions;
        }
        return null;
    }
}
