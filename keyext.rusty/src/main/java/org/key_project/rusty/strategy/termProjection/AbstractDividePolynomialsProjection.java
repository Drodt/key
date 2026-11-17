/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy.termProjection;

import java.math.BigInteger;

import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.feature.helper.arith.Monomial;
import org.key_project.prover.strategy.costbased.termProjection.ProjectionToTerm;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.op.AbstractTermTransformer;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.rule.metaconstruct.arith.RMonomial;

public abstract class AbstractDividePolynomialsProjection implements ProjectionToTerm {
    private final ProjectionToTerm leftCoefficient, polynomial;

    protected AbstractDividePolynomialsProjection(ProjectionToTerm leftCoefficient,
            ProjectionToTerm polynomial) {
        this.leftCoefficient = leftCoefficient;
        this.polynomial = polynomial;
    }

    @Override
    public Term toTerm(RuleApp app, PosInOccurrence pos, ProofGoal<?> goal, MutableState mState) {
        final Term coeffT = leftCoefficient.toTerm(app, pos, goal, mState);
        final Term polyT = polynomial.toTerm(app, pos, goal, mState);

        final Services services = ((Goal) goal).proof().getServices();
        final BigInteger coeff =
            new BigInteger(AbstractTermTransformer.convertToDecimalString(coeffT, services));

        return quotient(coeff, polyT, services);
    }

    protected abstract Term divide(Monomial numerator, BigInteger denominator, Services services);

    private Term quotient(BigInteger monoCoeff, Term rightPoly,
            Services services) {
        final Function add = services.getLDTs().getIntLDT().getAdd();
        if (rightPoly.op() == add) {
            final var left = quotient(monoCoeff, rightPoly.sub(0), services);
            final var right = quotient(monoCoeff, rightPoly.sub(1), services);
            return services.getTermBuilder().func(add, left, right);
        }

        final Monomial rightMono = RMonomial.create(rightPoly, services);
        return divide(rightMono, monoCoeff, services);
    }
}
