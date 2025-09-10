/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.prover.strategy.costbased.appcontainer;

import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.RuleAppCost;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.NonNull;

/**
 * Container for RuleApp instances with cost as determined by a given Strategy. Instances of this
 * class are immutable.
 */
public abstract class RuleAppContainer implements Comparable<RuleAppContainer> {
    /**
     * The stored rule app
     */
    private final RuleApp ruleApp;

    /**
     * The costs of the stored rule app
     */
    private final RuleAppCost cost;

    protected RuleAppContainer(RuleApp p_app, RuleAppCost p_cost) {
        ruleApp = p_app;
        cost = p_cost;
    }

    @Override
    public final int compareTo(RuleAppContainer o) {
        return cost.compareTo(o.cost);
    }

    /**
     * Create a list of new RuleAppContainers that are to be considered for application.
     */
    public abstract ImmutableList<RuleAppContainer> createFurtherApps(ProofGoal<?> p_goal);

    /**
     * Create a <code>RuleApp</code> that is suitable to be applied or <code>null</code>.
     */
    public abstract RuleApp completeRuleApp(ProofGoal<?> p_goal);

    protected final RuleApp getRuleApp() {
        return ruleApp;
    }


    public final RuleAppCost getCost() {
        return cost;
    }

    /**
     * Create container for a RuleApp.
     *
     * @return container for the currently applicable RuleApp, the cost may be an instance of
     *         <code>TopRuleAppCost</code>.
     */
    public static @NonNull RuleAppContainer createAppContainer(
            RuleApp p_app,
            PosInOccurrence p_pio,
            ProofGoal<?> p_goal) {

        return p_app.createRuleAppContainer(p_pio, p_goal, true);
    }

    /**
     * Create containers for RuleApps.
     *
     * @return list of containers for the currently applicable RuleApps, the cost may be an instance
     *         of <code>TopRuleAppCost</code>.
     */
    public static ImmutableList<RuleAppContainer> createAppContainers(
            ImmutableList<? extends RuleApp> rules,
            PosInOccurrence pos, ProofGoal<?> goal) {
        ImmutableList<RuleAppContainer> result = ImmutableSLList.nil();

        if (rules.size() == 1) {
            result = result.prepend(createAppContainer(rules.head(), pos, goal));
        } else if (rules.size() > 1) {
            for (RuleApp rule : rules) {
                // Used to have taclet apps at front and builtin apps at the end
                result = result.prepend(rule.createRuleAppContainer(pos, goal, true));
            }
        }
        return result;
    }
}
