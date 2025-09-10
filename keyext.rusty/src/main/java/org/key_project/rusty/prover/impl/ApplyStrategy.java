/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.prover.impl;

import java.util.concurrent.atomic.AtomicLong;

import org.key_project.prover.engine.GoalChooser;
import org.key_project.prover.engine.TaskStartedInfo;
import org.key_project.prover.engine.impl.ApplyStrategyInfo;
import org.key_project.prover.engine.impl.DefaultProver;
import org.key_project.prover.rules.RuleApp;
import org.key_project.rusty.proof.*;
import org.key_project.rusty.proof.proofevent.RuleAppInfo;
import org.key_project.rusty.settings.ProofSettings;
import org.key_project.rusty.settings.StrategySettings;
import org.key_project.rusty.strategy.StrategyProperties;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Applies rules in an automated fashion. The caller should ensure that the strategy runs in its
/// own
/// thread
/// [Uses code by Hans Muller and Kathy
/// Walrath](http://java.sun.com/products/jfc/tsc/articles/threads/threads2.html)
///
/// @author Richard Bubel, Daniel Drodt
public class ApplyStrategy extends DefaultProver<@NonNull Proof, Goal> {
    public static final Logger LOGGER = LoggerFactory.getLogger(ApplyStrategy.class);

    public static final AtomicLong PERF_GOAL_APPLY = new AtomicLong();

    /// The default [GoalChooser] to choose goals to which rules are applied if the
    /// [StrategySettings] of the proof provides no customized one.
    private final GoalChooser<Proof, Goal> defaultGoalChooser;

    /// interrupted by the user?
    private boolean autoModeActive = false;

    // Please create this object beforehand and re-use it.
    // Otherwise, the addition/removal of the InteractiveProofListener
    // can cause a ConcurrentModificationException during ongoing operation
    public ApplyStrategy(GoalChooser<Proof, Goal> defaultGoalChooser) {
        this.defaultGoalChooser = defaultGoalChooser;
    }

    private void init(Proof newProof, ImmutableList<Goal> goals, int maxSteps, long timeout) {
        this.proof = newProof;
        maxApplications = maxSteps;
        this.timeout = timeout;
        countApplied = 0;
        closedGoals = 0;
        cancelled = false;
        stopCondition = proof.getSettings().getStrategySettings().getApplyStrategyStopCondition();
        assert stopCondition != null;
        goalChooser = getGoalChooserForProof(proof);
        assert goalChooser != null;
        goalChooser.init(newProof, goals);
        setAutoModeActive(true);
        fireTaskStarted(
            new DefaultTaskStartedInfo(TaskStartedInfo.TaskKind.Strategy, PROCESSING_STRATEGY,
                stopCondition.getMaximalWork(maxSteps, timeout)));
    }

    @Override
    protected final @Nullable RuleApp updateBuiltInRuleIndex(Goal goal, @Nullable RuleApp app) {
        // Hack: built in rules may become applicable without BuiltInRuleAppIndex noticing---->
        if (app == null) {
            goal.ruleAppIndex().scanBuiltInRules(goal);
            app = goal.getRuleAppManager().next();
        }
        // <-------
        return app;
    }

    @Override
    public synchronized @NonNull ApplyStrategyInfo<@NonNull Proof, Goal> start(Proof proof,
            Goal goal) {
        return start(proof, ImmutableSLList.<Goal>nil().prepend(goal));
    }

    @Override
    public synchronized @NonNull ApplyStrategyInfo<@NonNull Proof, Goal> start(Proof proof,
            @NonNull ImmutableList<Goal> goals) {
        ProofSettings settings = proof.getSettings();
        StrategySettings stratSet = settings.getStrategySettings();
        return start(proof, goals, stratSet);
    }

    @Override
    public synchronized @NonNull ApplyStrategyInfo<@NonNull Proof, Goal> start(Proof proof,
            @NonNull ImmutableList<Goal> goals, Object strategySettings) {
        final StrategySettings stratSet = (StrategySettings) strategySettings;
        int maxSteps = stratSet.getMaxSteps();
        long timeout = stratSet.getTimeout();

        boolean stopAtFirstNonCloseableGoal = proof.getSettings().getStrategySettings()
                .getActiveStrategyProperties().getProperty(StrategyProperties.STOPMODE_OPTIONS_KEY)
                .equals(StrategyProperties.STOPMODE_NONCLOSE);
        return start(proof, goals, maxSteps, timeout, stopAtFirstNonCloseableGoal);
    }

    @Override
    public synchronized @NonNull ApplyStrategyInfo<@NonNull Proof, Goal> start(Proof proof,
            @NonNull ImmutableList<Goal> goals, int maxSteps, long timeout,
            boolean stopAtFirstNonCloseableGoal) {
        assert proof != null;

        this.stopAtFirstNonClosableGoal = stopAtFirstNonCloseableGoal;

        ProofTreeListener treeListener = prepareStrategy(proof, goals, maxSteps, timeout);
        ApplyStrategyInfo<@NonNull Proof, Goal> result = executeStrategy(treeListener);
        finishStrategy(result);
        return result;
    }

    @Override
    public void clear() {
        final GoalChooser<@NonNull Proof, Goal> goalChooser = getGoalChooserForProof(proof);
        proof = null;
        if (goalChooser != null) {
            goalChooser.init(null, ImmutableSLList.nil());
        }
    }

    @Override
    public boolean hasBeenInterrupted() {
        return cancelled;
    }

    private boolean isAutoModeActive() {
        return autoModeActive;
    }

    private void setAutoModeActive(boolean autoModeActive) {
        this.autoModeActive = autoModeActive;
    }

    private void finishStrategy(ApplyStrategyInfo<Proof, Goal> result) {
        assert result != null; // CS
        proof.addAutoModeTime(result.getTime());
        fireTaskFinished(new DefaultTaskFinishedInfo(this, result, proof, result.getTime(),
            result.getNumberOfAppliedRuleApps(), result.getNumberOfClosedGoals()));
    }

    private ProofTreeListener prepareStrategy(Proof proof, ImmutableList<Goal> goals, int maxSteps,
            long timeout) {
        ProofTreeListener treeListener = new ProofTreeAdapter() {
            @Override
            public void proofGoalsAdded(ProofTreeEvent e) {
                Iterable<Goal> newGoals = e.getGoals();
                // Check for a closed goal ...
                if (!newGoals.iterator().hasNext()) {
                    // No new goals have been generated ...
                    closedGoals++;
                }
            }
        };
        proof.addProofTreeListener(treeListener);
        init(proof, goals, maxSteps, timeout);

        return treeListener;
    }

    private ApplyStrategyInfo<Proof, Goal> executeStrategy(ProofTreeListener treeListener) {
        assert proof != null;

        ProofListener pl = new ProofListener();
        proof.addRuleAppListener(pl);
        ApplyStrategyInfo<Proof, Goal> result;
        try {
            result = doWork(goalChooser, stopCondition);
        } finally {
            proof.removeProofTreeListener(treeListener);
            proof.removeRuleAppListener(pl);
            setAutoModeActive(false);
        }
        return result;
    }

    /// Returns the [GoalChooser] to use for the given [Proof]. This is the custom one
    /// defined in the proof's [StrategySettings] or the default one of this
    /// [#defaultGoalChooser] otherwise.
    ///
    /// @param proof The [Proof] for which an [GoalChooser] is required.
    /// @return The [GoalChooser] to use.
    private GoalChooser<@NonNull Proof, Goal> getGoalChooserForProof(Proof proof) {
        GoalChooser<@NonNull Proof, Goal> chooser = null;
        if (proof != null) {
            chooser = proof.getSettings().getStrategySettings().getCustomApplyStrategyGoalChooser();
        }
        return chooser != null ? chooser : defaultGoalChooser;
    }

    private class ProofListener implements RuleAppListener {

        /// invoked when a rule has been applied
        @Override
        public void ruleApplied(ProofEvent e) {
            if (!isAutoModeActive()) {
                return;
            }
            RuleAppInfo rai = e.getRuleAppInfo();
            if (rai == null) {
                return;
            }

            final GoalChooser<Proof, Goal> goalChooser =
                getGoalChooserForProof(rai.getOriginalNode().proof());
            synchronized (goalChooser) {
                // reverse just to keep old order
                goalChooser.updateGoalList(rai.getOriginalNode(), e.getNewGoals().reverse());
            }
        }
    }
}
