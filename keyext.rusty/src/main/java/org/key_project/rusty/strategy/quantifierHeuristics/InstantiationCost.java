/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy.quantifierHeuristics;

import org.key_project.logic.Term;
import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.RuleAppCost;
import org.key_project.prover.strategy.costbased.feature.Feature;
import org.key_project.prover.strategy.costbased.quantifierHeuristics.TriggerTreatment;
import org.key_project.prover.strategy.costbased.termProjection.ProjectionToTerm;
import org.key_project.rusty.Services;


/// Feature that returns the number of branches after instantiated the quantifier formula.
public class InstantiationCost implements Feature {
    final private ProjectionToTerm varInst;

    /// how much the prediction is told about the theories
    private final TriggerTreatment treatment;

    private InstantiationCost(ProjectionToTerm var, TriggerTreatment treatment) {
        varInst = var;
        this.treatment = treatment;
    }

    public static Feature create(ProjectionToTerm varInst, TriggerTreatment treatment) {
        return new InstantiationCost(varInst, treatment);
    }

    /// Compute the cost of a RuleApp.
    @Override
    public RuleAppCost computeCost(RuleApp app,
            PosInOccurrence pos, ProofGoal<?> goal, MutableState mState) {
        assert pos != null : "Projection is only applicable to rules with find";

        final Term formula = pos.sequentFormula().formula();
        final var instance =
            varInst.toTerm(app, pos, (org.key_project.rusty.proof.Goal) goal, mState);

        return Instantiation.computeCost(instance, formula, goal.sequent(),
            (Services) goal.proof().getServices(), treatment);
    }
}
