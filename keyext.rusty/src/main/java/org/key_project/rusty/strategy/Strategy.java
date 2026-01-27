/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.strategy;

import org.key_project.logic.Named;
import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.rules.RuleSet;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.RuleAppCost;
import org.key_project.prover.strategy.costbased.feature.Feature;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.settings.ProofSettings;

import org.jspecify.annotations.NonNull;

/// Generic interface for evaluating the cost of a RuleApp with regard to a specific strategy
public interface Strategy<Goal extends ProofGoal<@NonNull Goal>> extends Named, Feature {
    /// Evaluate the cost of a <code>RuleApp</code>. Starts a new independent computation.
    ///
    /// @param app the RuleApp
    /// @param pos position where <code>app</code> is to be applied
    /// @param goal the goal on which <code>app</code> is to be applied
    /// @return the cost of the rule application expressed as a
    /// <code>RuleAppCost</code> object. <code>TopRuleAppCost.INSTANCE</code>
    /// indicates that the rule shall not be applied at all (it is discarded by
    /// the strategy).
    default RuleAppCost computeCost(RuleApp app, PosInOccurrence pos, Goal goal) {
        return computeCost(app, pos, goal, new MutableState());
    }

    /// Checks if the [Strategy] should stop at the first non-closeable [Goal].
    ///
    /// @return `true` stop, `false` continue on other [Goal]s.
    boolean isStopAtFirstNonCloseableGoal();

    /// Re-Evaluate a <code>RuleApp</code>. This method is called immediately before a rule is
    /// really
    /// applied
    ///
    /// @return true iff the rule should be applied, false otherwise
    boolean isApprovedApp(RuleApp app, PosInOccurrence pio, Goal goal);

    /// Instantiate an incomplete <code>RuleApp</code>. This method is called when the
    /// <code>AutomatedRuleApplicationManager</code> comes across a rule application in which some
    /// schema variables are not yet instantiated, or which is in some other way incomplete. The
    /// strategy then has the opportunity to return/provide a list of (more) complete rule
    /// applications by feeding them into the provided <code>RuleAppCostCollector</code>.
    void instantiateApp(RuleApp app, PosInOccurrence pio, Goal goal,
            RuleAppCostCollector collector);

    /// Updates the [Strategy] for the given [Proof] by setting the [Strategy]'s
    /// [StrategyProperties] to the given ones.
    ///
    /// @param proof The [Proof] the strategy of which should be updated.
    /// @param p The new [StrategyProperties]
    static void updateStrategySettings(Proof proof, StrategyProperties p) {
        final Strategy<org.key_project.rusty.proof.Goal> strategy = proof.getActiveStrategy();
        ProofSettings.DEFAULT_SETTINGS.getStrategySettings().setStrategy(strategy.name());
        ProofSettings.DEFAULT_SETTINGS.getStrategySettings().setActiveStrategyProperties(p);

        proof.getSettings().getStrategySettings().setStrategy(strategy.name());
        proof.getSettings().getStrategySettings().setActiveStrategyProperties(p);

        proof.setActiveStrategy(strategy);
    }

    default boolean isResponsibleFor(RuleSet rs) { return false; }

    default RuleAppCost instantiateApp(RuleApp app, PosInOccurrence pio,
            org.key_project.rusty.proof.Goal goal,
            MutableState mState) {
        return null;
    }
}
