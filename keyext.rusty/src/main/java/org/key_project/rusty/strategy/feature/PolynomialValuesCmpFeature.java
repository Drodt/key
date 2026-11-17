/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy.feature;

import java.math.BigInteger;

import org.key_project.logic.Term;
import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.ITacletApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.feature.BinaryTacletAppFeature;
import org.key_project.prover.strategy.costbased.feature.Feature;
import org.key_project.prover.strategy.costbased.feature.helper.arith.Polynomial;
import org.key_project.prover.strategy.costbased.termProjection.ProjectionToTerm;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.op.AbstractTermTransformer;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.rule.metaconstruct.arith.RPolynomial;

/// Return zero only if the value of one (left) polynomial always will be (less or equal) or (less)
/// than the value of a second (right) polynomial. Both polynomials can optionally be multiplied
/// with some constant before comparison.
public abstract class PolynomialValuesCmpFeature extends BinaryTacletAppFeature {
    private final ProjectionToTerm left, right, leftCoeff, rightCoeff;

    protected PolynomialValuesCmpFeature(ProjectionToTerm left, ProjectionToTerm right,
            ProjectionToTerm leftCoeff, ProjectionToTerm rightCoeff) {
        this.left = left;
        this.right = right;
        this.leftCoeff = leftCoeff;
        this.rightCoeff = rightCoeff;
    }

    public static Feature lt(ProjectionToTerm left, ProjectionToTerm right) {
        return lt(left, right, null, null);
    }

    public static Feature lt(ProjectionToTerm left, ProjectionToTerm right,
            ProjectionToTerm leftCoeff, ProjectionToTerm rightCoeff) {
        return new PolynomialValuesCmpFeature(left, right, leftCoeff, rightCoeff) {
            @Override
            protected boolean compare(Polynomial leftPoly, Polynomial rightPoly) {
                return leftPoly.valueLess(rightPoly);
            }
        };
    }

    public static Feature leq(ProjectionToTerm left, ProjectionToTerm right) {
        return leq(left, right, null, null);
    }

    public static Feature leq(ProjectionToTerm left, ProjectionToTerm right,
            ProjectionToTerm leftCoeff, ProjectionToTerm rightCoeff) {
        return new PolynomialValuesCmpFeature(left, right, leftCoeff, rightCoeff) {
            @Override
            protected boolean compare(Polynomial leftPoly, Polynomial rightPoly) {
                return leftPoly.valueLeq(rightPoly);
            }
        };
    }

    public static Feature eq(ProjectionToTerm left, ProjectionToTerm right) {
        return eq(left, right, null, null);
    }

    public static Feature eq(ProjectionToTerm left, ProjectionToTerm right,
            ProjectionToTerm leftCoeff, ProjectionToTerm rightCoeff) {
        return new PolynomialValuesCmpFeature(left, right, leftCoeff, rightCoeff) {
            @Override
            protected boolean compare(Polynomial leftPoly, Polynomial rightPoly) {
                return leftPoly.valueEq(rightPoly);
            }
        };
    }

    public static Feature divides(ProjectionToTerm left, ProjectionToTerm right) {
        return divides(left, right, null, null);
    }

    public static Feature divides(ProjectionToTerm left, ProjectionToTerm right,
            ProjectionToTerm leftCoeff, ProjectionToTerm rightCoeff) {
        return new PolynomialValuesCmpFeature(left, right, leftCoeff, rightCoeff) {
            @Override
            protected boolean compare(Polynomial leftPoly, Polynomial rightPoly) {
                // we currently only support constant polynomials
                assert leftPoly.getParts().isEmpty();
                assert rightPoly.getParts().isEmpty();
                if (leftPoly.getConstantTerm().signum() == 0) {
                    return true;
                }
                if (rightPoly.getConstantTerm().signum() == 0) {
                    return false;
                }
                return leftPoly.getConstantTerm().mod(rightPoly.getConstantTerm().abs())
                        .signum() == 0;
            }
        };
    }

    @Override
    protected boolean filter(ITacletApp app, PosInOccurrence pos,
            ProofGoal<?> goal, MutableState mState) {
        return compare(getPolynomial(left, leftCoeff, app, pos, goal, mState),
            getPolynomial(right, rightCoeff, app, pos, goal, mState));
    }

    protected abstract boolean compare(Polynomial leftPoly, Polynomial rightPoly);

    private Polynomial getPolynomial(ProjectionToTerm polyProj,
            ProjectionToTerm coeffProj,
            ITacletApp app, PosInOccurrence pos, ProofGoal<?> goal, MutableState mState) {
        final Services services = ((Goal) goal).proof().getServices();
        final Polynomial poly =
            RPolynomial.create(polyProj.toTerm(app, pos, goal, mState), services);

        if (coeffProj == null) {
            return poly;
        }
        final Term coeffT = coeffProj.toTerm(app, pos, goal, mState);
        if (coeffT == null) {
            return poly;
        }

        final BigInteger coeff =
            new BigInteger(AbstractTermTransformer.convertToDecimalString(coeffT, services));
        return poly.multiply(coeff);
    }
}
