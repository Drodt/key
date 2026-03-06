/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.control;

import org.key_project.prover.engine.ProverTaskListener;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.rule.BuiltInRule;
import org.key_project.rusty.rule.TacletApp;
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
    boolean isMinimizeInteraction();

    void setMinimizeInteraction(boolean minimizeInteraction);

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

    /// collects all applicable RewriteTaclets of the current goal (called by the SequentViewer)
    ///
    /// @return a list of Taclets with all applicable RewriteTaclets
    ImmutableList<TacletApp> getRewriteTaclet(Goal focusedGoal,
            PosInOccurrence pos);

    /// collects all applicable FindTaclets of the current goal (called by the SequentViewer)
    ///
    /// @return a list of Taclets with all applicable FindTaclets
    ImmutableList<TacletApp> getFindTaclet(Goal focusedGoal,
            PosInOccurrence pos);

    /// collects all applicable NoFindTaclets of the current goal (called by the SequentViewer)
    ///
    /// @return a list of Taclets with all applicable NoFindTaclets
    ImmutableList<TacletApp> getNoFindTaclet(Goal focusedGoal);

    /// collects all built-in rules that are applicable at the given sequent position 'pos'.
    ///
    /// @param pos the PosInSequent where to look for applicable rules
    ImmutableList<BuiltInRule> getBuiltInRule(Goal focusedGoal, PosInOccurrence pos);
}
