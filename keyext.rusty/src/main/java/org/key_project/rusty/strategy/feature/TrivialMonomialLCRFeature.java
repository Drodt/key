/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.strategy.feature;

import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.feature.Feature;
import org.key_project.prover.strategy.costbased.termProjection.ProjectionToTerm;
import org.key_project.rusty.Services;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.rule.TacletApp;
import org.key_project.rusty.rule.metaconstruct.arith.Monomial;

/// Return zero of the least common reducible of two monomials is so trivial that it is not
/// necessary
/// to do the critical pair completion
///
/// "A critical-pair/completion algorithm for finitely generated ideals in rings"
public class TrivialMonomialLCRFeature extends BinaryTacletAppFeature {
    private final ProjectionToTerm<Goal> a, b;

    private TrivialMonomialLCRFeature(ProjectionToTerm<Goal> a, ProjectionToTerm<Goal> b) {
        this.a = a;
        this.b = b;
    }

    public static Feature create(ProjectionToTerm<Goal> a, ProjectionToTerm<Goal> b) {
        return new TrivialMonomialLCRFeature(a, b);
    }

    @Override
    protected boolean filter(TacletApp app, PosInOccurrence pos, Goal goal, MutableState mState) {
        final Services services = goal.proof().getServices();
        final Monomial aMon = Monomial.create(a.toTerm(app, pos, goal, mState), services);
        final Monomial bMon = Monomial.create(b.toTerm(app, pos, goal, mState), services);

        return aMon.variablesAreCoprime(bMon);
    }
}
