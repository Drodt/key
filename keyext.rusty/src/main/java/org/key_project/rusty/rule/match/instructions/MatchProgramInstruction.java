/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
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

/// The _monolithic_ program matcher: matches the Java program of a modality by delegating to
/// the pattern's own `ProgramElement.match(SourceData, MatchConditions)`, which walks the
/// program AST with the per-construct `match` methods of the AST classes. The current element
/// is the modality's [RustyBlock]; the whole program is consumed in this one instruction.
///
/// It is the safety net of the interpreter back-end: it matches a program the converted
/// instructions do not cover (conversion off, the default, or a construct outside the
/// single-source dispatch, e.g. a variable-arity list schema variable), and it is the main consumer
/// of the AST `match` methods.
public class MatchProgramInstruction implements MatchInstruction {
    private final RustyProgramElement pe;

    public MatchProgramInstruction(RustyProgramElement pe) {
        this.pe = pe;
    }

    @Override
    public MatchResultInfo match(SyntaxElement actualElement, MatchResultInfo matchConditions,
            LogicServices services) {
        return pe.match(
            new SourceData(((RustyBlock) actualElement).program(), -1,
                (Services) services),
            (MatchConditions) matchConditions);
    }
}
