/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.rule.match.instructions;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.logic.op.sv.OperatorSV;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.matcher.vm.instruction.*;
import org.key_project.rusty.ast.RustyProgramElement;
import org.key_project.rusty.logic.op.ParametricFunctionInstance;
import org.key_project.rusty.logic.op.sv.ModalOperatorSV;
import org.key_project.rusty.logic.op.sv.ProgramSV;
import org.key_project.rusty.logic.op.sv.VariableSV;
import org.key_project.rusty.logic.sort.*;
import org.key_project.util.collection.ImmutableList;

/// The factory for the Java-DL match instructions: static creation methods for every instruction
/// kind used when a find pattern's matcher is built (schema variables by kind, term labels,
/// programs, bound-variable binding, cursor moves, and the generic identity / node-kind checks from
/// the framework). Both back-ends obtain their instructions here: the interpreter emits them into
/// its instruction stream, the compiled matcher applies element-based ones directly.
///
/// Note for authors of new instructions: the proof-search queue indexes waiting rule applications
/// by the operator families their `\\assumes` formulas could match. The families are stated
/// by the match heads themselves
/// ([org.key_project.prover.rules.matcher.compiler.MatchHead#topOperatorDescriptor()], an
/// abstract method, so a new head cannot omit the decision) and, for the operators of concrete
/// terms, by [org.key_project.rusty.rule.match.RustMatchPlanBuilder#matchFamilyOf]. An
/// instruction that accepts more than one operator in a position, like
/// [#getSimilarParametricFunctionInstruction], belongs to a head whose descriptor names the
/// family, and `matchFamilyOf` must agree with it (pinned by
/// `QueueRuleApplicationManagerWakeKeyTest`).
public class RustyDLMatchVMInstructionSet {
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

    public static MatchInstruction matchAndBindVariables(
            ImmutableList<QuantifiableVariable> boundVars) {
        return new BindVariablesInstruction(boundVars);
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

    public static MatchInstruction getMatchInstructionForGenericArgument(GenericArgument arg) {
        // ensures that dispatch does not happen in matching code
        return switch (arg) {
            case SortArg(GenericSort gs) -> new MatchGenericSortInstruction(gs);
            case SortArg(ParametricSortInstance psi) when psi.containsGenericSort() ->
                new MatchParametricSortInstruction(psi);
            case SortArg(var sort) -> new MatchBySortIdentityInstruction(sort);
            case TermArg ta -> new MatchTermArgInstruction(ta);
            default -> throw new IllegalArgumentException("Unknown generic argument: " + arg);
        };
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
}
