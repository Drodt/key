/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.rule.match.instructions;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.prover.rules.matcher.vm.instruction.MatchInstruction;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.RustyProgramElement;
import org.key_project.rusty.ast.SourceData;
import org.key_project.rusty.logic.RustyBlock;
import org.key_project.rusty.rule.MatchConditions;

public class MatchProgramInstruction implements MatchInstruction {
    private final RustyProgramElement pe;

    public MatchProgramInstruction(RustyProgramElement pe) {
        this.pe = pe;
    }

    @Override
    public MatchResultInfo match(SyntaxElement actualElement, MatchResultInfo matchConditions,
            LogicServices services) {
        final var rb = (RustyBlock) actualElement;
        final MatchResultInfo result = pe.match(
            new SourceData(rb.program(), -1, (Services) services),
            (MatchConditions) matchConditions);
        return result;
    }
}
