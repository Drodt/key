/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.control;


import org.key_project.prover.engine.ProverTaskListener;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.proof.ProofEvent;
import org.key_project.rusty.util.ProofStarter;
import org.key_project.util.collection.ImmutableList;

import org.jspecify.annotations.Nullable;

/// The default implementation of [ProofControl].
///
/// @author Martin Hentschel
public class DefaultProofControl extends AbstractProofControl {
    /// The [UserInterfaceControl] in which this [ProofControl] is used.
    private final UserInterfaceControl ui;

    /// The currently running [Thread].
    private @Nullable Thread autoModeThread;

    /// Constructor.
    ///
    /// @param ui The [UserInterfaceControl] in which this [ProofControl] is used.
    /// @param defaultProverTaskListener The default [ProverTaskListener] which will be added
    /// to all started [ApplyStrategy] instances.
    public DefaultProofControl(UserInterfaceControl ui,
            DefaultUserInterfaceControl defaultProverTaskListener) {
        super(defaultProverTaskListener);
        this.ui = ui;
    }

    @Override
    public synchronized void startAutoMode(Proof proof, ImmutableList<Goal> goals,
            ProverTaskListener ptl) {
        if (!isInAutoMode()) {
            autoModeThread = new AutoModeThread(proof, goals, ptl);
            autoModeThread.start();
        }
    }

    @Override
    public synchronized void stopAutoMode() {
        if (isInAutoMode()) {
            autoModeThread.interrupt();
        }
    }

    @Override
    public void waitWhileAutoMode() {
        while (isInAutoMode()) { // Wait until auto mode has stopped.
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public boolean isInAutoMode() {
        return autoModeThread != null;
    }

    private class AutoModeThread extends Thread {
        private final Proof proof;

        private final ImmutableList<Goal> goals;

        private final ProverTaskListener ptl;

        public AutoModeThread(Proof proof, ImmutableList<Goal> goals, ProverTaskListener ptl) {
            this.proof = proof;
            this.goals = goals;
            this.ptl = ptl;
        }

        @Override
        public void run() {
            try {
                fireAutoModeStarted(new ProofEvent(proof));
                ProofStarter starter = ptl != null
                        ? new ProofStarter(
                            new CompositePTListener(getDefaultProverTaskListener(), ptl), false)
                        : new ProofStarter(getDefaultProverTaskListener(), false);
                starter.init(proof);
                if (goals != null) {
                    starter.start(goals);
                } else {
                    starter.start();
                }
            } finally {
                autoModeThread = null;
                fireAutoModeStopped(new ProofEvent(proof));
            }
        }
    }
}
