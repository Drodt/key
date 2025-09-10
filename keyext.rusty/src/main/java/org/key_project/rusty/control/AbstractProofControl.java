/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.control;

import java.util.LinkedList;
import java.util.List;

import org.key_project.prover.engine.ProverTaskListener;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.proof.ProofEvent;
import org.key_project.util.collection.ImmutableList;

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
}
