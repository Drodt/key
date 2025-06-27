/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.rule.match.instructions;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.OperatorSV;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.rusty.logic.op.sv.ProgramSV;
import org.key_project.rusty.logic.op.sv.VariableSV;

/**
 * Matching VM instruction that matches all operator schema variables that
 * are not a {@link VariableSV} or a {@link ProgramSV}.
 * For those see {@link MatchVariableSVInstruction} and {@link MatchProgramSVInstruction}.
 */
public class MatchNonVariableSVInstruction extends MatchSchemaVariableInstruction {
    public MatchNonVariableSVInstruction(OperatorSV op) {
        super(op);
        assert !(op instanceof VariableSV || op instanceof ProgramSV);
    }

    @Override
    public MatchResultInfo match(SyntaxElement actualElement, MatchResultInfo mc,
            LogicServices services) {
        return addInstantiation((Term) actualElement, mc, services);
    }
}
