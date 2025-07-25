/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.macros;

import java.util.Set;

import de.uka.ilkd.key.logic.op.ObserverFunction;
import de.uka.ilkd.key.logic.op.UpdateApplication;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.rule.OneStepSimplifier;
import de.uka.ilkd.key.strategy.*;

import org.key_project.logic.Name;
import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.Rule;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.rules.RuleSet;
import org.key_project.prover.rules.Taclet;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.NumberRuleAppCost;
import org.key_project.prover.strategy.costbased.RuleAppCost;
import org.key_project.prover.strategy.costbased.TopRuleAppCost;

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
        public Name name() {
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
        public <Goal extends ProofGoal<@NonNull Goal>> RuleAppCost computeCost(RuleApp app,
                PosInOccurrence pio, Goal p_goal,
                MutableState mState) {

            final var goal = (de.uka.ilkd.key.proof.Goal) p_goal;
            Rule rule = app.rule();
            if (FinishSymbolicExecutionMacro.isForbiddenRule(rule)) {
                return TopRuleAppCost.INSTANCE;
            }

            if (modalityCache.hasModality(goal.node().sequent())) {
                return delegate.computeCost(app, pio, goal, mState);
            }

            if (isAdmittedRule(rule)) {
                return NumberRuleAppCost.getZeroCost();
            }

            // apply OSS to <inv>() calls.
            if (rule instanceof OneStepSimplifier) {
                var target = pio.subTerm();
                if (target.op() instanceof UpdateApplication) {
                    var updatedOp = target.sub(1).op();
                    if (updatedOp instanceof ObserverFunction) {
                        return NumberRuleAppCost.getZeroCost();
                    }
                }
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
