/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.macro;

import java.util.Set;

import org.key_project.logic.Name;
import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.rules.RuleSet;
import org.key_project.prover.rules.Taclet;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.NumberRuleAppCost;
import org.key_project.prover.strategy.costbased.RuleAppCost;
import org.key_project.prover.strategy.costbased.TopRuleAppCost;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.rule.Rule;
import org.key_project.rusty.strategy.RuleAppCostCollector;
import org.key_project.rusty.strategy.Strategy;

import org.jspecify.annotations.NonNull;

public class AutoPilotPrepareProofMacro extends StrategyProofMacro {
    private static final Set<String> ADMITTED_RULES =
        Set.of(new String[] { "orRight", "impRight", "close", "andRight" });
    private static final Set<String> ADMITTED_RULE_SETS =
        Set.of(new String[] { "update_elim", "update_join" });

    public AutoPilotPrepareProofMacro() { super(); }

    @Override
    public String getName() {
        return "Auto Pilot (Preparation Only)";
    }

    @Override
    public String getCategory() {
        return "Auto Pilot";
    }

    @Override
    public String getDescription() {
        return "<html><ol><li>Finish symbolic execution" + "<li>Separate proof obligations"
            + "<li>Expand invariant definitions</ol>";
    }

    @Override
    public String getScriptCommandName() {
        return "autopilot-prep";
    }

    public static boolean isAdmittedRule(Rule rule) {
        String name = rule.name().toString();
        if (ADMITTED_RULES.contains(name)) {
            return true;
        }

        if (rule instanceof Taclet taclet) {
            for (RuleSet rs : taclet.getRuleSets()) {
                if (ADMITTED_RULE_SETS.contains(rs.name().toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static class AutoPilotStrategy implements Strategy<Goal> {
        private static final Name NAME = new Name("Autopilot filter strategy");
        private final Strategy<@NonNull Goal> delegate;
        /** the modality cache used by this strategy */
        private final ModalityCache modalityCache = new ModalityCache();

        public AutoPilotStrategy(Proof proof) {
            this.delegate = proof.getActiveStrategy();
        }

        @Override
        public @NonNull Name name() {
            return NAME;
        }

        @Override
        public boolean isApprovedApp(RuleApp app, PosInOccurrence pio, Goal goal) {
            return computeCost(app, pio, goal, new MutableState()) != TopRuleAppCost.INSTANCE &&
            // Assumptions are normally not considered by the cost
            // computation, because they are normally not yet
            // instantiated when the costs are computed. Because the
            // application of a rule sometimes makes sense only if
            // the assumptions are instantiated in a particular way
            // (for instance equalities should not be applied on
            // themselves), we need to give the delegate the possibility
            // to reject the application of a rule by calling
            // isApprovedApp. Otherwise, in particular equalities may
            // be applied on themselves.
                    delegate.isApprovedApp(app, pio, goal);
        }

        @Override
        public <G extends ProofGoal<@NonNull G>> RuleAppCost computeCost(RuleApp app,
                PosInOccurrence pio, G p_goal,
                MutableState mState) {

            final var goal = (Goal) p_goal;
            Rule rule = (Rule) app.rule();
            if (FinishSymbolicExecutionMacro.isForbiddenRule(rule)) {
                return TopRuleAppCost.INSTANCE;
            }

            if (modalityCache.hasModality(goal.getNode().sequent())) {
                return delegate.computeCost(app, pio, goal, mState);
            }

            if (isAdmittedRule(rule)) {
                return NumberRuleAppCost.getZeroCost();
            }

            return TopRuleAppCost.INSTANCE;
        }

        @Override
        public void instantiateApp(RuleApp app, PosInOccurrence pio,
                Goal goal,
                RuleAppCostCollector collector) {
            delegate.instantiateApp(app, pio, goal, collector);
        }

        @Override
        public boolean isStopAtFirstNonCloseableGoal() {
            return false;
        }
    }

    @Override
    protected Strategy<@NonNull Goal> createStrategy(Proof proof,
            PosInOccurrence posInOcc) {
        return new AutoPilotStrategy(proof);
    }
}
