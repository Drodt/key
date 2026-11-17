/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy.termProjection;

import org.key_project.logic.Term;
import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.feature.StableCost;
import org.key_project.prover.strategy.costbased.feature.helper.arith.Monomial;
import org.key_project.prover.strategy.costbased.termProjection.ProjectionToTerm;
import org.key_project.rusty.Services;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.rule.metaconstruct.arith.RMonomial;

@StableCost
public final class ReduceMonomialsProjection implements ProjectionToTerm {
    private final ProjectionToTerm dividend, divisor;

    private ReduceMonomialsProjection(ProjectionToTerm dividend,
            ProjectionToTerm divisor) {
        this.dividend = dividend;
        this.divisor = divisor;
    }

    public static ProjectionToTerm create(ProjectionToTerm dividend,
            ProjectionToTerm divisor) {
        return new ReduceMonomialsProjection(dividend, divisor);
    }

    @Override
    public Term toTerm(RuleApp app, PosInOccurrence pos, ProofGoal<?> goal, MutableState mState) {
        final Term dividendT = dividend.toTerm(app, pos, goal, mState);
        final Term divisorT = divisor.toTerm(app, pos, goal, mState);

        final Services services = ((Goal) goal).proof().getServices();
        final Monomial mDividend = RMonomial.create(dividendT, services);
        final Monomial mDivisor = RMonomial.create(divisorT, services);

        return RMonomial.toTerm(mDivisor.reduce(mDividend), services);
    }
}
