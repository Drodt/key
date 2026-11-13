/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.rule.match.instructions;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.prover.rules.matcher.vm.VMProgramInterpreter;
import org.key_project.prover.rules.matcher.vm.instruction.MatchInstruction;
import org.key_project.rusty.logic.RustyBlock;

import org.jspecify.annotations.Nullable;

/// Matches the Java program of a modality by running a sub-program of `VMInstruction`s over
/// the program tree (with its own cursor), instead of the monolithic
/// `MatchProgramInstruction` which delegates to the separate `ProgramElement.match` AST
/// matcher. The current element is the modality's [RustyBlock] (as for
/// `MatchProgramInstruction`); the sub-program runs on its `program()`, leaving the
/// outer cursor at the `JavaBlock` so the surrounding navigation is unchanged.
public final class MatchSubProgramInstruction implements MatchInstruction {
    private final VMProgramInterpreter program;

    public MatchSubProgramInstruction(VMProgramInterpreter program) {
        this.program = program;
    }

    @Override
    public @Nullable MatchResultInfo match(SyntaxElement actualElement,
            MatchResultInfo matchConditions, LogicServices services) {
        return program.match(((RustyBlock) actualElement).program(), matchConditions, services);
    }
}
