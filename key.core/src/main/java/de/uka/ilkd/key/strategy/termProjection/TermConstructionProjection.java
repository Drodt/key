/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.strategy.termProjection;

import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.op.Modality;
import de.uka.ilkd.key.logic.op.Operator;
import de.uka.ilkd.key.proof.Goal;

import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.termProjection.ProjectionToTerm;

/**
 * Term projection for constructing a bigger term from a sequence of direct subterms and an
 * operator.
 * <p>
 * NB: this is a rather restricted version of term construction, one can think of also allowing
 * bound variables, etc to be specified
 */
public class TermConstructionProjection implements ProjectionToTerm<Goal> {

    private final Operator op;
    private final ProjectionToTerm<Goal>[] subTerms;


    private TermConstructionProjection(Operator op, ProjectionToTerm<Goal>[] subTerms) {
        assert !(op instanceof Modality); // XXX
        this.op = op;
        this.subTerms = subTerms;
        assert op.arity() == subTerms.length;
    }

    public static ProjectionToTerm<Goal> create(Operator op, ProjectionToTerm<Goal>[] subTerms) {
        return new TermConstructionProjection(op, subTerms);
    }

    @Override
    public Term toTerm(RuleApp app, PosInOccurrence pos, Goal goal, MutableState mState) {
        final Term[] subs = new Term[subTerms.length];
        for (int i = 0; i != subTerms.length; ++i) {
            subs[i] = (Term) subTerms[i].toTerm(app, pos, goal, mState);
        }
        return goal.proof().getServices().getTermFactory().createTerm(op, subs, null, null);
    }

}
