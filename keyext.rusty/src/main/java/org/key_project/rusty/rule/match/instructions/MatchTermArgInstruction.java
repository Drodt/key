/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.rule.match.instructions;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.prover.rules.matcher.vm.MatchProgram;
import org.key_project.prover.rules.matcher.vm.instruction.MatchInstruction;
import org.key_project.rusty.logic.sort.TermArg;
import org.key_project.rusty.rule.match.RustMatchPlanBuilder;

import org.jspecify.annotations.Nullable;

public class MatchTermArgInstruction implements MatchInstruction {
    private final MatchProgram prog;

    MatchTermArgInstruction(TermArg ta) {
        prog = RustMatchPlanBuilder.getCompiledProgram(ta.term());
    }

    @Override
    public @Nullable MatchResultInfo match(SyntaxElement actualElement,
            MatchResultInfo matchConditions, LogicServices services) {
        if (!(actualElement instanceof TermArg(org.key_project.logic.Term term)))
            return null;
        return prog.match(term, matchConditions, services);
    }
}
