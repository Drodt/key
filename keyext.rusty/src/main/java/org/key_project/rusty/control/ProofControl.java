/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.control;

import org.key_project.prover.engine.ProverTaskListener;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Proof;
import org.key_project.util.collection.ImmutableList;

/// A [ProofControl] provides the user interface independent logic to apply rules on a proof.
/// This includes:
///
/// - Functionality to reduce the available rules ([#isMinimizeInteraction()] and
/// [#setMinimizeInteraction(boolean)]).
/// - Functionality to list available rules.
/// - Functionality to apply a rule interactively.
/// - Functionality to apply rules by the auto mode synchronous or asynchronously in a different
/// [Thread].
/// - Functionality to execute a macro.
///
/// @author Martin Hentschel
public interface ProofControl {
    /// Starts the auto mode for the given [Proof].
    ///
    /// @param proof The [Proof] to start auto mode of.
    void startAutoMode(Proof proof);

    /// Requests to stop the current auto mode without blocking the current [Thread] until the
    /// auto mode has stopped.
    void stopAutoMode();

    /// Starts the auto mode for the given [Proof] and the given [Goal]s.
    ///
    /// @param proof The [Proof] to start auto mode of.
    /// @param goals The [Goal]s to close.
    void startAutoMode(Proof proof, ImmutableList<Goal> goals);

    /// Starts the auto mode for the given proof which must be contained in this user interface and
    /// blocks the current thread until it has finished.
    ///
    /// @param proof The [Proof] to start auto mode and to wait for.
    void startAndWaitForAutoMode(Proof proof);

    /// Blocks the current [Thread] while the auto mode of this [UserInterfaceControl] is
    /// active.
    void waitWhileAutoMode();

    /// Checks if the auto mode is currently running.
    ///
    /// @return `true` auto mode is running, `false` auto mode is not running.
    boolean isInAutoMode();

    /// Returns the default [ProverTaskListener] which will be added to all started
    /// [ApplyStrategy] instances.
    ///
    /// @return The default [ProverTaskListener] which will be added to all started
    /// [ApplyStrategy] instances.
    ProverTaskListener getDefaultProverTaskListener();
}
