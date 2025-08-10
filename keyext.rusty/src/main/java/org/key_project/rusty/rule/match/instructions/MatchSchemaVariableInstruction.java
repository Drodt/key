/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.rule.match.instructions;

import org.key_project.logic.LogicServices;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.OperatorSV;
import org.key_project.prover.rules.instantiation.IllegalInstantiationException;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.prover.rules.matcher.vm.instruction.MatchInstruction;
import org.key_project.rusty.ast.RustyProgramElement;
import org.key_project.rusty.rule.inst.SVInstantiations;

import org.jspecify.annotations.NonNull;

import static org.key_project.rusty.logic.equality.RenamingTermProperty.RENAMING_TERM_PROPERTY;

public abstract class MatchSchemaVariableInstruction
        implements MatchInstruction {
    protected final @NonNull OperatorSV op;

    protected MatchSchemaVariableInstruction(@NonNull OperatorSV op) {
        this.op = op;
    }

    /// Tries to add the pair <tt>(this,term)</tt> to the match conditions. If successful the
    /// resulting conditions are returned, otherwise null. Failure is possible e.g. if this
    /// schemavariable has been already matched to a term <tt>t2</tt> which is not unifiable with
    /// the
    /// given term.
    protected final MatchResultInfo addInstantiation(Term term,
            MatchResultInfo matchCond,
            LogicServices services) {
        if (op.isRigid() && !term.isRigid()) {
            return null;
        }

        final SVInstantiations inst = (SVInstantiations) matchCond.getInstantiations();

        final Term t = inst.getTermInstantiation(op, services);
        if (t != null) {
            if (!RENAMING_TERM_PROPERTY.equalsModThisProperty(t, term)) {
                return null;
            } else {
                return matchCond;
            }
        }

        try {
            return matchCond.setInstantiations(inst.add(op, term, services));
        } catch (IllegalInstantiationException e) {
            return null;
        }
    }

    /// tries to match the schema variable of this instruction with the specified
    /// [RustyProgramElement] `instantiationCandidate` w.r.t. the given constraints by
    /// [MatchResultInfo]
    ///
    /// @param instantiationCandidate the [RustyProgramElement] to be matched
    /// @param mc the [MatchResultInfo] with additional constraints (e.g. previous matches of
    /// this instructions [SchemaVariable])
    /// @param services the [Services]
    /// @return `null` if no matches have been found or the new [MatchResultInfo] with
    /// the pair ([SchemaVariable], [RustyProgramElement]) added
    public MatchResultInfo match(RustyProgramElement instantiationCandidate,
            MatchResultInfo mc,
            LogicServices services) {
        return null;
    }
}
