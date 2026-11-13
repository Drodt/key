/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.rule.match;

import java.util.List;

import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.matcher.compiler.MatchHead;
import org.key_project.prover.rules.matcher.vm.MatchProgram;
import org.key_project.prover.rules.matcher.vm.instruction.MatchInstruction;
import org.key_project.prover.rules.matcher.vm.instruction.VMInstruction;
import org.key_project.rusty.logic.op.ElementaryUpdate;
import org.key_project.rusty.logic.op.ProgramVariable;

import org.jspecify.annotations.Nullable;

import static org.key_project.rusty.rule.match.instructions.RustyDLMatchVMInstructionSet.*;

/// Match head for an [ElementaryUpdate] `lhs := value` (the update operator assigning
/// a value to a program variable): it matches the operator and the left-hand side; the value
/// subterm is matched by the enclosing
/// [org.key_project.prover.rules.matcher.compiler.OperatorPlan].
public final class ElementaryUpdateHead implements MatchHead {
    /// the pattern's update operator; the operator family in [#topOperatorDescriptor] and
    /// the name in [#toString].
    private final ElementaryUpdate op;
    private final MatchInstruction lhsMatcher;
    /// whether the left-hand side is a schema variable (it advances by sibling, not by descent).
    private final boolean lhsIsSchemaVariable;

    private ElementaryUpdateHead(ElementaryUpdate op, MatchInstruction lhsMatcher,
            boolean lhsIsSchemaVariable) {
        this.op = op;
        this.lhsMatcher = lhsMatcher;
        this.lhsIsSchemaVariable = lhsIsSchemaVariable;
    }

    /// @param elUp the elementary update pattern
    /// @return a head for `elUp`, or `null` if its left-hand side is neither a schema
    /// variable nor a concrete location variable (then the caller falls back)
    public static @Nullable ElementaryUpdateHead of(ElementaryUpdate elUp) {
        if (elUp.lhs() instanceof SchemaVariable sv) {
            return new ElementaryUpdateHead(elUp, getMatchInstructionForSV(sv), true);
        } else if (elUp.lhs() instanceof ProgramVariable pv) {
            return new ElementaryUpdateHead(elUp, getMatchIdentityInstruction(pv), false);
        }
        return null;
    }

    @Override
    public void emit(List<VMInstruction> out) {
        out.add(getCheckNodeKindInstruction(ElementaryUpdate.class));
        out.add(gotoNextInstruction());
        out.add(lhsMatcher);
        out.add(lhsIsSchemaVariable ? gotoNextSiblingInstruction() : gotoNextInstruction());
    }

    @Override
    public MatchProgram compileHeadCheck() {
        final MatchInstruction lhs = lhsMatcher;
        return (element, mc,
                services) -> ((Term) element).op() instanceof ElementaryUpdate actualElUp
                        ? lhs.match(actualElUp.lhs(), mc, services)
                        : null;
    }

    @Override
    public @Nullable ElementaryUpdate topOperatorDescriptor() {
        // interned per left-hand side: a concrete lhs fixes one operator, a schema-variable
        // lhs accepts many, which no single family describes
        return lhsIsSchemaVariable ? null : op;
    }

    @Override
    public String toString() {
        return "elemUpdate(" + op.name() + ")";
    }
}
