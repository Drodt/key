/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.rule.match.instructions;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.logic.op.sv.OperatorSV;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.matcher.vm.instruction.*;
import org.key_project.prover.rules.matcher.vm.instruction.MatchInstruction;
import org.key_project.rusty.ast.RustyProgramElement;
import org.key_project.rusty.logic.op.ParametricFunctionInstance;
import org.key_project.rusty.logic.op.sv.ModalOperatorSV;
import org.key_project.rusty.logic.op.sv.ProgramSV;
import org.key_project.rusty.logic.op.sv.VariableSV;
import org.key_project.rusty.logic.sort.GenericSort;

/// Class encoding the instructions of the matching vm
public class RustyDLMatchInstructionSet {
    public static GotoNextInstruction gotoNextInstruction() {
        return GotoNextInstruction.INSTANCE;
    }

    public static GotoNextSiblingInstruction gotoNextSiblingInstruction() {
        return GotoNextSiblingInstruction.INSTANCE;
    }

    public static MatchModalOperatorSVInstruction matchModalOperatorSV(
            ModalOperatorSV sv) {
        return new MatchModalOperatorSVInstruction(sv);
    }

    public static MatchSchemaVariableInstruction matchNonVariableSV(OperatorSV sv) {
        return new MatchNonVariableSVInstruction(sv);
    }

    public static MatchSchemaVariableInstruction matchVariableSV(
            VariableSV sv) {
        return new MatchVariableSVInstruction(sv);
    }

    public static MatchSchemaVariableInstruction matchProgramSV(
            ProgramSV sv) {
        return new MatchProgramSVInstruction(sv);
    }

    public static MatchInstruction matchProgram(RustyProgramElement prg) {
        return new MatchProgramInstruction(prg);
    }

    /// returns the instruction for the specified variable
    ///
    /// @param op the [SchemaVariable] for which to get the instruction
    /// @return the instruction for the specified variable
    public static MatchSchemaVariableInstruction getMatchInstructionForSV(
            SchemaVariable op) {
        return switch (op) {
            case VariableSV variableSV -> matchVariableSV(variableSV);
            case ProgramSV programSV -> matchProgramSV(programSV);
            case OperatorSV operatorSV -> matchNonVariableSV(operatorSV);
            default -> throw new IllegalArgumentException(
                "Do not know how to match " + op + " of type " + op.getClass());
        };
    }

    public static SimilarParametricFunctionInstruction getSimilarParametricFunctionInstruction(
            ParametricFunctionInstance psi) {
        return new SimilarParametricFunctionInstruction(psi);
    }

    public static MatchIdentityInstruction getMatchIdentityInstruction(
            SyntaxElement syntaxElement) {
        return new MatchIdentityInstruction(syntaxElement);
    }

    public static MatchGenericSortInstruction getMatchGenericSortInstruction(GenericSort gs) {
        return new MatchGenericSortInstruction(gs);
    }

    public static CheckNodeKindInstruction getCheckNodeKindInstruction(
            Class<? extends SyntaxElement> kind) {
        return new CheckNodeKindInstruction(kind);
    }

    public static MatchInstruction matchAndBindVariable(
            QuantifiableVariable var) {
        return BindVariablesInstruction.create(var);
    }
}
