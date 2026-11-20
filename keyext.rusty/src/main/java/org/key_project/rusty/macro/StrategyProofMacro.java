/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.macro;

import java.util.List;
import java.util.stream.Collectors;

import org.key_project.prover.engine.GoalChooser;
import org.key_project.prover.engine.ProofSearchInformation;
import org.key_project.prover.engine.ProverCore;
import org.key_project.prover.engine.ProverTaskListener;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.RuleApplicationManager;
import org.key_project.prover.strategy.Strategy;
import org.key_project.rusty.control.UserInterfaceControl;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Node;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.prover.impl.AutoProvers;
import org.key_project.rusty.strategy.FocussedRuleApplicationManager;
import org.key_project.util.collection.ImmutableList;

import org.jspecify.annotations.NonNull;

public abstract class StrategyProofMacro extends AbstractProofMacro {
    protected abstract Strategy<Goal> createStrategy(Proof proof, PosInOccurrence posInOcc);

    /// Whether this macro's run may use the multicore prover.
    ///
    /// The strategy returned by [#createStrategy] is installed proof-wide, so under the
    /// multicore prover all workers share the one instance and call it concurrently. Macros whose
    /// strategy keeps cross-goal mutable state (a step counter, a breakpoint flag, discovered merge
    /// points) have inherently sequential semantics: with several workers the outcome would depend
    /// on scheduling, breaking the requirement that the multi-core prover produce the same proof as
    /// the single-threaded one. Such macros override this to return `false` and run on the
    /// single-threaded prover.
    protected boolean allowParallel() {
        return true;
    }

    /// {@inheritDoc}
    /// This macro can always be applied (does not change anything perhaps)
    /// TODO make this only applicable if it has an impact.
    @Override
    public boolean canApplyTo(Proof proof, ImmutableList<Goal> goals,
            PosInOccurrence posInOcc) {
        return goals != null && !goals.isEmpty();
    }

    /// Subclasses can use this method to do some postprocessing on the proof-object after the
    /// strategy has finished.
    ///
    /// @param proof The proof object.
    protected void doPostProcessing(Proof proof) {}

    /*
     * Set a new rule app manager similar to the focussed mode. Set a new strategy which only allows
     * for the named admitted rules. Then run automation mode and in the end reset the managers. and
     * the strategy.
     *
     * If the automation is interrupted, report the interruption as an exception.
     */
    @Override
    public ProofMacroFinishedInfo applyTo(UserInterfaceControl uic, Proof proof,
            ImmutableList<Goal> goals, PosInOccurrence posInOcc, ProverTaskListener listener)
            throws InterruptedException {
        if (goals == null || goals.isEmpty()) {
            // should not happen, because in this case canApplyTo returns
            // false
            return null;
        }
        List<Node> nodes = goals.stream().map(Goal::getNode).collect(Collectors.toList());

        final GoalChooser<Proof, Goal> goalChooser =
            proof.getInitConfig().getProfile().<Proof, Goal>getSelectedGoalChooserBuilder()
                    .create();
        final ProverCore<Proof, Goal> applyStrategy =
            AutoProvers.create(goalChooser, proof.getInitConfig().getProfile(), allowParallel());
        final ImmutableList<Goal> ignoredOpenGoals = setDifference(proof.openGoals(), goals);

        //
        // The observer to handle the progress bar
        final ProofMacroListener pml =
            new ProgressBarListener(goals.size(), getMaxSteps(proof), listener);
        applyStrategy.addProverTaskObserver(pml);
        // add a focus manager if there is a focus
        if (posInOcc != null) {
            RuleApplicationManager<Goal> realManager;
            FocussedRuleApplicationManager manager;
            for (Goal goal : goals) {
                realManager = goal.getRuleAppManager();
                realManager.clearCache();
                manager = new FocussedRuleApplicationManager(realManager, goal, posInOcc);
                goal.setRuleAppManager(manager);
            }
        }

        // set a new strategy.
        Strategy<@NonNull Goal> oldStrategy = proof.getActiveStrategy();
        proof.setActiveStrategy(createStrategy(proof, posInOcc));

        ProofMacroFinishedInfo info;
        try {
            // find the relevant goals
            // and start
            final ProofSearchInformation<Proof, Goal> result = applyStrategy.start(proof, goals);
            if (result.isError()) {
                throw new RuntimeException("Proof search failed: " + result.getException(),
                    result.getException());
            }
            synchronized (applyStrategy) { // wait for applyStrategy to finish its last rule
                // application
                if (applyStrategy.hasBeenInterrupted()) { // reraise interrupted exception if
                    // necessary
                    throw new InterruptedException();
                }
            }
        } finally {
            // this resets the proof strategy and the managers after the automation
            // has run
            for (final Goal openGoal : proof.openGoals()) {
                RuleApplicationManager<Goal> manager = openGoal.getRuleAppManager();
                // touch the manager only if necessary
                if (manager instanceof FocussedRuleApplicationManager fram) {
                    manager = fram.rootManager;
                    manager.clearCache();
                    openGoal.setRuleAppManager(fram);
                }
            }
            final ImmutableList<Goal> resultingGoals =
                setDifference(proof.openGoals(), ignoredOpenGoals);
            info = new ProofMacroFinishedInfo(this, resultingGoals,
                nodes);
            proof.setActiveStrategy(oldStrategy);
            doPostProcessing(proof);
            applyStrategy.removeProverTaskObserver(pml);
        }
        return info;
    }

    private static ImmutableList<Goal> setDifference(ImmutableList<Goal> goals1,
            ImmutableList<Goal> goals2) {
        ImmutableList<Goal> difference = goals1;
        for (Goal goal : goals2) {
            difference = difference.removeFirst(goal);
        }
        return difference;
    }
}
