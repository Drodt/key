/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.macro;

import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.RuleAppCost;
import org.key_project.prover.strategy.costbased.TopRuleAppCost;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.strategy.RuleAppCostCollector;
import org.key_project.rusty.strategy.Strategy;

import org.jspecify.annotations.NonNull;

public abstract class FilterStrategy implements Strategy<@NonNull Goal> {
    private final Strategy<@NonNull Goal> delegate;

    protected FilterStrategy(Strategy<@NonNull Goal> delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isApprovedApp(RuleApp app, PosInOccurrence pio,
            Goal goal) {
        return delegate.isApprovedApp(app, pio, goal);
    }

    @Override
    public <G extends ProofGoal<@NonNull G>> RuleAppCost computeCost(RuleApp app,
            PosInOccurrence pio,
            G goal, MutableState mState) {
        if (!isApprovedApp(app, pio, (Goal) goal)) {
            return TopRuleAppCost.INSTANCE;
        }
        return delegate.computeCost(app, pio, goal, mState);
    }

    @Override
    public void instantiateApp(RuleApp app, PosInOccurrence pio, Goal goal,
            RuleAppCostCollector collector) {
        delegate.instantiateApp(app, pio, goal, collector);
    }
}
