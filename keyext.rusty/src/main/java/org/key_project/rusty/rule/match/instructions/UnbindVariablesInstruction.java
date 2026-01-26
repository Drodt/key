/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.rule.match.instructions;

import org.key_project.logic.LogicServices;
import org.key_project.logic.PoolSyntaxElementCursor;
import org.key_project.logic.SyntaxElement;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.prover.rules.matcher.vm.instruction.MatchInstruction;
import org.key_project.rusty.rule.MatchConditions;

import org.jspecify.annotations.Nullable;

public class UnbindVariablesInstruction implements MatchInstruction {
    private final int size;

    public UnbindVariablesInstruction(int size) {
        this.size = size;
    }

    @Override
    public @Nullable MatchResultInfo match(SyntaxElement actualElement,
            MatchResultInfo matchConditions, LogicServices services) {
        return matchConditions;
    }

    @Override
    public @Nullable MatchResultInfo match(PoolSyntaxElementCursor cursor,
            MatchResultInfo matchResultInfo, LogicServices services) {
        var mc = (MatchConditions) matchResultInfo;
        for (int i = 0; i < size; i++) {
            mc = mc.shrinkLogicVarTable();
        }
        return mc;
    }
}
