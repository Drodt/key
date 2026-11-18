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
import org.key_project.prover.strategy.costbased.NumberRuleAppCost;
import org.key_project.prover.strategy.costbased.RuleAppCost;
import org.key_project.prover.strategy.costbased.feature.Feature;
import org.key_project.prover.strategy.costbased.quantifierHeuristics.TriggerTreatment;
import org.key_project.prover.strategy.costbased.termProjection.ProjectionToTerm;
import org.key_project.rusty.Services;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.strategy.StrategyProperties;
import org.key_project.rusty.strategy.quantifierHeuristics.tiebreak.GenPolTieBreak;
import org.key_project.rusty.strategy.quantifierHeuristics.tiebreak.PolarityTieBreak;
import org.key_project.rusty.strategy.quantifierHeuristics.tiebreak.QuantifierInstantiationTieBreak;

/// Orders quantifier instantiation candidates whose predicted cost is equal, so the choice within a
/// cost band falls to the content of the instance instead of the position of the quantified
/// formula.
/// The ordering signal follows the quantifier treatment: `Best`
/// ([StrategyProperties#TRIGGERS_BEST]) uses [PolarityTieBreak], `Good`
/// ([StrategyProperties#TRIGGERS_GOOD]) uses [GenPolTieBreak], and `Classic`
/// ([StrategyProperties#TRIGGERS_CLASSIC]) adds no tie-break: instances keep the order of
/// their positions in the sequent. The values stay far below the distance of the predicted cost
/// bands, so the prediction
/// itself is never overridden.
public class InstantiationTieBreakFeature implements Feature {
    private final ProjectionToTerm varInst;
    /// the resolved tie-break, or `null` for the classic option
    private final QuantifierInstantiationTieBreak strategy;

    private InstantiationTieBreakFeature(ProjectionToTerm var,
            QuantifierInstantiationTieBreak strategy) {
        varInst = var;
        this.strategy = strategy;
    }

    /// The feature for the given setting of the trigger option, resolved once at strategy
    /// construction like every other strategy option.
    ///
    /// @param varInst the projection to the candidate instance
    /// @param triggersOption the value of [StrategyProperties#TRIGGERS_OPTIONS_KEY]
    /// @return the feature
    public static Feature create(ProjectionToTerm varInst, String triggersOption) {
        final QuantifierInstantiationTieBreak strategy = switch (triggersOption) {
            case StrategyProperties.TRIGGERS_GOOD -> GenPolTieBreak.INSTANCE;
            case StrategyProperties.TRIGGERS_CLASSIC -> null;
            default -> PolarityTieBreak.INSTANCE;
        };
        return new InstantiationTieBreakFeature(varInst, strategy);
    }

    @Override
    public RuleAppCost computeCost(RuleApp app,
            PosInOccurrence pos, ProofGoal<?> goal, MutableState mState) {
        assert pos != null : "Projection is only applicable to rules with find";

        final Goal rgoal = (Goal) goal;
        if (strategy == null) {
            // the classic treatment orders instances by their position in the sequent alone
            return NumberRuleAppCost.getZeroCost();
        }

        final Term formula = pos.sequentFormula().formula();
        final Term instance = varInst.toTerm(app, pos, rgoal, mState);
        return Instantiation.computeTieBreak(instance, formula, goal.sequent(), rgoal,
            (Services) goal.proof().getServices(), TriggerTreatment.BEST, strategy);
    }
}
