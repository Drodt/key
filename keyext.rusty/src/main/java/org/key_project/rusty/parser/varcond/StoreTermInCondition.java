/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.varcond;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.rusty.Services;
import org.key_project.rusty.rule.LightweightSyntacticalReplaceVisitor;
import org.key_project.rusty.rule.inst.SVInstantiations;

/// Stores the given [Term], after substitution of [SchemaVariable]s, into the given
/// [SchemaVariable] for later use in other conditions and transformers.
///
/// @author Dominic Steinhoefel
public class StoreTermInCondition implements VariableCondition {
    private final SchemaVariable storeInSV;
    private final Term term;

    public StoreTermInCondition(SchemaVariable resultVarSV, Term term) {
        this.storeInSV = resultVarSV;
        this.term = term;
    }

    @Override
    public MatchResultInfo check(SchemaVariable var, SyntaxElement instCandidate,
            MatchResultInfo matchCond, LogicServices lServices) {
        final var services = (Services) lServices;
        final var svInst = (SVInstantiations) matchCond.getInstantiations();

        if (svInst.getInstantiation(storeInSV) != null) {
            return matchCond;
        }

        final var replVisitor = new LightweightSyntacticalReplaceVisitor(svInst, services);
        term.execPostOrder(replVisitor);
        final Term instantiatedTerm = replVisitor.getTerm();

        return matchCond.setInstantiations(
            svInst.add(storeInSV, instantiatedTerm, services));
    }

    @Override
    public String toString() {
        return String.format( //
            "\\varcond (\\storeTermIn(%s, %s))", storeInSV, term);
    }
}
