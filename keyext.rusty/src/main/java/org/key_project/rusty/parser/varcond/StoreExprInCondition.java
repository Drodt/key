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
import org.key_project.rusty.ast.expr.BlockExpression;
import org.key_project.rusty.ast.expr.Expr;
import org.key_project.rusty.ast.stmt.ExpressionStatement;
import org.key_project.rusty.logic.op.RModality;
import org.key_project.rusty.logic.op.sv.ProgramSV;
import org.key_project.rusty.rule.LightweightSyntacticalReplaceVisitor;
import org.key_project.rusty.rule.inst.SVInstantiations;

/// Stores the given [org.key_project.rusty.ast.expr.Expr], after substitution of
/// [SchemaVariable]s, into the given
/// [ProgramSV] for later use in other conditions and transformers. The arguments are a
/// [ProgramSV] and a [Term], where the [Term] must contain a
/// [org.key_project.rusty.logic.RustyBlock]
/// with a [org.key_project.rusty.ast.expr.BlockExpression] containing <emph>a single
/// expression</emph> (that works, e.g., when
/// passing an expression like
/// <code>\modality{#allmodal}{ loop s#body }\endmodality(post)</code>); this expr is then
/// stored (in the example the loop expr).
///
/// @author Dominic Steinhoefel
public class StoreExprInCondition implements VariableCondition {
    private final ProgramSV storeInSV;
    private final Term term;

    public StoreExprInCondition(ProgramSV storeInSV, Term term) {
        this.term = term;
        this.storeInSV = storeInSV;
    }

    @Override
    public MatchResultInfo check(SchemaVariable var, SyntaxElement instCandidate,
            MatchResultInfo matchCond, LogicServices lServices) {
        final var services = (Services) lServices;
        final var svInst = (SVInstantiations) matchCond.getInstantiations();

        if (svInst.getInstantiation(storeInSV) != null) {
            return matchCond;
        }

        final LightweightSyntacticalReplaceVisitor replVisitor =
            new LightweightSyntacticalReplaceVisitor(svInst, services);
        term.execPostOrder(replVisitor);
        final Term instantiatedTerm = replVisitor.getTerm();

        // We assume that the term has a RustyBlock and that consists of a BlockExpression
        // containing exactly one expression statement or one expression; see JavaDoc.

        var mod = (RModality) instantiatedTerm.op();
        var be = (BlockExpression) mod.programBlock().program();
        Expr expr;
        if (be.getStatements().isEmpty() && be.getValue() != null) {
            expr = be.getValue();
        } else if (be.getStatements().size() == 1 && be.getValue() == null
                && be.getStatements().get(0) instanceof ExpressionStatement es) {
            expr = es.getExpression();
        } else {
            throw new IllegalStateException(
                "Expected a block with one expression or one expression statement, got: " + be);
        }

        return matchCond.setInstantiations(svInst.add(storeInSV, expr, services));
    }
}
