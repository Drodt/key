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
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.SourceData;
import org.key_project.rusty.ast.expr.ContextBlockExpression;
import org.key_project.rusty.logic.RustyBlock;
import org.key_project.rusty.rule.MatchConditions;

import org.jspecify.annotations.Nullable;

/// Matches the Java program of a modality whose program is a [ContextBlockExpression] (the
/// `.. ...` pattern that is ubiquitous in symbolic-execution taclets). The context
/// bookkeeping (variable-length prefix descent, inner execution context, prefix/suffix positions)
/// is performed by [ContextBlockExpression#match]; the active statements are matched by the
/// supplied VM sub-program. The current element is the modality's [RustyBlock] (as for
/// `MatchProgramInstruction`).
///
/// @see ContextBlockExpression#match(SourceData, MatchConditions,
/// org.key_project.prover.rules.matcher.vm.ProgramChildrenMatcher)
public final class MatchContextBlockExpressionInstruction implements MatchInstruction {
    private final ContextBlockExpression contextBlock;
    private final VMProgramInterpreter activeStatements;

    public MatchContextBlockExpressionInstruction(ContextBlockExpression contextBlock,
            VMProgramInterpreter activeStatements) {
        this.contextBlock = contextBlock;
        this.activeStatements = activeStatements;
    }

    @Override
    public @Nullable MatchResultInfo match(SyntaxElement actualElement,
            MatchResultInfo matchConditions, LogicServices services) {
        return contextBlock.match(
            new SourceData(((RustyBlock) actualElement).program(), -1, (Services) services),
            (MatchConditions) matchConditions, activeStatements);
    }
}
