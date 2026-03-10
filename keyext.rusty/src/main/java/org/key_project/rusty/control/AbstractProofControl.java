/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.control;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.key_project.prover.engine.ProverTaskListener;
import org.key_project.prover.rules.Taclet;
import org.key_project.prover.rules.instantiation.AssumesFormulaInstSeq;
import org.key_project.prover.rules.instantiation.AssumesFormulaInstantiation;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.proof.ProofEvent;
import org.key_project.rusty.rule.BuiltInRule;
import org.key_project.rusty.rule.NoPosTacletApp;
import org.key_project.rusty.rule.RuleApp;
import org.key_project.rusty.rule.TacletApp;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

/// Provides a basic implementation of [ProofControl].
///
/// @author Martin Hentschel
public abstract class AbstractProofControl implements ProofControl {
    /// Optionally, the [RuleCompletionHandler] to use.
    private final RuleCompletionHandler ruleCompletionHandler;

    /// The default [ProverTaskListener] which will be added to all started
    /// [ApplyStrategy] instances.
    private final ProverTaskListener defaultProverTaskListener;

    /// Contains all available [AutoModeListener].
    private final List<AutoModeListener> autoModeListener = new LinkedList<>();

    private boolean minimizeInteraction; // minimize user interaction

    /// Constructor.
    ///
    /// @param defaultProverTaskListener The default [ProverTaskListener] which will be added
    /// to all started [ApplyStrategy] instances.
    protected AbstractProofControl(ProverTaskListener defaultProverTaskListener) {
        this(defaultProverTaskListener, null);
    }

    /// Constructor.
    ///
    /// @param defaultProverTaskListener The default [ProverTaskListener] which will be added
    /// to all started [ApplyStrategy] instances.
    /// @param ruleCompletionHandler An optional [RuleCompletionHandler].
    protected AbstractProofControl(ProverTaskListener defaultProverTaskListener,
            RuleCompletionHandler ruleCompletionHandler) {
        this.ruleCompletionHandler = ruleCompletionHandler;
        this.defaultProverTaskListener = defaultProverTaskListener;
    }

    @Override
    public boolean isMinimizeInteraction() {
        return minimizeInteraction;
    }

    @Override
    public void setMinimizeInteraction(boolean minimizeInteraction) {
        this.minimizeInteraction = minimizeInteraction;
    }

    /// fires the event that automatic execution has started
    protected void fireAutoModeStarted(ProofEvent e) {
        AutoModeListener[] listener =
            autoModeListener.toArray(new AutoModeListener[0]);
        for (AutoModeListener aListenerList : listener) {
            aListenerList.autoModeStarted(e);
        }
    }

    /// fires the event that automatic execution has stopped
    protected void fireAutoModeStopped(ProofEvent e) {
        AutoModeListener[] listener =
            autoModeListener.toArray(new AutoModeListener[0]);
        for (AutoModeListener aListenerList : listener) {
            aListenerList.autoModeStopped(e);
        }
    }

    /// {@inheritDoc}
    @Override
    public void startAutoMode(Proof proof) {
        startAutoMode(proof, proof.openEnabledGoals());
    }

    /// {@inheritDoc}
    @Override
    public void startAndWaitForAutoMode(Proof proof) {
        startAutoMode(proof);
        waitWhileAutoMode();
    }

    /// {@inheritDoc}
    @Override
    public synchronized void startAutoMode(Proof proof, ImmutableList<Goal> goals) {
        startAutoMode(proof, goals, null);
    }

    protected abstract void startAutoMode(Proof proof, ImmutableList<Goal> goals,
            ProverTaskListener ptl);

    /// {@inheritDoc}
    @Override
    public ProverTaskListener getDefaultProverTaskListener() {
        return defaultProverTaskListener;
    }

    @Override
    public ImmutableList<TacletApp> getNoFindTaclet(Goal focusedGoal) {
        return filterTaclet(focusedGoal, focusedGoal.ruleAppIndex()
                .getNoFindTaclet(focusedGoal.proof().getServices()),
            null);
    }

    @Override
    public ImmutableList<TacletApp> getFindTaclet(Goal focusedGoal,
            PosInOccurrence pos) {
        if (pos != null && focusedGoal != null) {
            return filterTaclet(focusedGoal,
                focusedGoal.ruleAppIndex().getFindTaclet(pos), pos);
        }
        return ImmutableSLList.nil();
    }

    @Override
    public ImmutableList<TacletApp> getRewriteTaclet(Goal focusedGoal, PosInOccurrence pos) {
        if (pos != null) {
            return filterTaclet(focusedGoal,
                focusedGoal.ruleAppIndex().getRewriteTaclet(pos), pos);
        }

        return ImmutableSLList.nil();
    }

    @Override
    public ImmutableList<BuiltInRule> getBuiltInRule(Goal focusedGoal, PosInOccurrence pos) {
        ImmutableList<BuiltInRule> rules = ImmutableSLList.nil();

        for (RuleApp ruleApp : focusedGoal.ruleAppIndex()
                .getBuiltInRules(focusedGoal, pos)) {
            BuiltInRule r = (BuiltInRule) ruleApp.rule();
            if (!rules.contains(r)) {
                rules = rules.prepend(r);
            }
        }
        return rules;
    }

    /// takes NoPosTacletApps as arguments and returns a duplicate free list of the contained
    /// TacletApps
    private ImmutableList<TacletApp> filterTaclet(Goal focusedGoal,
            ImmutableList<NoPosTacletApp> tacletInstances,
            PosInOccurrence pos) {
        HashSet<Taclet> applicableRules = new HashSet<>();
        ImmutableList<TacletApp> result = ImmutableSLList.nil();
        for (NoPosTacletApp app : tacletInstances) {
            if (isMinimizeInteraction()) {
                ImmutableList<TacletApp> ifCandidates = app.findIfFormulaInstantiations(
                    focusedGoal.sequent(), focusedGoal.proof().getServices());
                if (ifCandidates.isEmpty()) {
                    continue; // skip this app
                }
                if (ifCandidates.size() == 1 && pos != null) {
                    TacletApp a = ifCandidates.head();
                    ImmutableList<AssumesFormulaInstantiation> ifs =
                        a.assumesFormulaInstantiations();
                    if (ifs != null && ifs.size() == 1
                            && ifs.head() instanceof AssumesFormulaInstSeq ifis) {
                        if (ifis.toPosInOccurrence().equals(pos.topLevel())) {
                            continue; // skip app if find and if same formula
                        }
                    }
                }
            }

            Taclet taclet = app.taclet();
            if (!applicableRules.contains(taclet)) {
                applicableRules.add(taclet);
                result = result.prepend(app);
            }
        }
        return result;
    }
}
