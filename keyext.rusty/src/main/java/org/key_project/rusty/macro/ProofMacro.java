/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.macro;

import org.key_project.prover.engine.ProverTaskListener;
import org.key_project.prover.engine.TaskFinishedInfo;
import org.key_project.prover.engine.TaskStartedInfo;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.rusty.control.UserInterfaceControl;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Node;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.prover.impl.DefaultTaskStartedInfo;
import org.key_project.util.collection.ImmutableList;

/// The interface ProofMacro is the entry point to a general strategy extension system.
///
/// ### Idea
/// Doing interaction with KeY is often tedious, many steps have to be performed over and over
/// again.
/// To facilitate the interaction, this framework allows a developer to define "macro strategy
/// steps" which combine many individual steps and are helpful in an interactive verification
/// attempt.
/// This interface is kept deliberately separate from many of the other mechanisms to remain open on
/// how to implement the macro.
///
/// ### Usage
/// Proof macros are meant to be stateless singletons.
/// Whenever a situation arises where the user wants to apply macros, they are asked whether they
/// can
/// be applied ( [#canApplyTo(Node, PosInOccurrence)],
/// [#canApplyTo(Proof, ImmutableList, PosInOccurrence)]).
/// A macro is offered to the user iff it returns <code>true</code>. No changes should be made
/// there.
/// A macro is then applied using
/// [#applyTo(UserInterfaceControl, Node, PosInOccurrence,
/// ProverTaskListener)]/
/// [#applyTo(UserInterfaceControl, Proof, ImmutableList, PosInOccurrence, ProverTaskListener)].
/// This may change the proof by applying rule applications. It is allowed to use automatic runs,
/// manual instantiations,
/// ...
/// A proof macro needs to extract all necessary information on the application from the mediator
/// passed to one
/// [#applyTo(UserInterfaceControl, Node, PosInOccurrence, ProverTaskListener)]
/// or
/// [#applyTo(UserInterfaceControl, Proof, ImmutableList, PosInOccurrence, ProverTaskListener)].
/// You will be able to access any interesting data from that starting point,
/// especially KeYMediator,getInteractiveProver().
///
/// ### Registration
/// When implementing a new proof macro, no existing code needs to be adapted. Please add the class
/// name of your new implementation to the file
/// <tt>resources/META-INF/services/org.key_project.rusty.macros.ProofMacro</tt>.
/// (see also `KeYMediator`)
///
/// @author mattias ulbrich
public interface ProofMacro {
    /// Gets the name of this macro.
    /// Used as menu entry
    ///
    /// @return a non-<code>null</code> constant string
    String getName();

    /// Gets a unique short name for this macro that can be used in proof scripts.
    /// If <code>null</code> is returned, the macro cannot be addressed from within scripts.
    ///
    /// @return <code>null</code> if not supported, or a non-<code>null</code> constant string as
    /// the
    /// short name
    String getScriptCommandName();

    /// Gets the category of this macro.
    /// Used as name of the menu under which the macro is sorted. Return <code>null</code> if no
    /// submenu is to be created.
    ///
    /// @return a constant string, or <code>null</code>
    String getCategory();

    /// Gets the description of this macro.
    /// Used as tooltip.
    ///
    /// @return a non-<code>null</code> constant string
    String getDescription();

    /// Checks whether this [ProofMacro] has a parameter named <code>paramName</code>. For use
    /// in proof scripts.
    ///
    /// @param paramName The name to check.
    /// @return true iff this [ProofMacro] has a parameter named <code>paramName</code>.
    boolean hasParameter(String paramName);

    /// Sets the parameter named <code>paramName</code> to the given String representation in
    /// <code>paramValue</code>. For use in proof scripts.
    ///
    /// @param paramName The name of the parameter.
    /// @param paramValue The value of the parameter.
    /// @throws IllegalArgumentException if there is no parameter of that name or the value is
    /// incorrectly formatted (e.g., cannot be converted to a number).
    void setParameter(String paramName, String paramValue) throws IllegalArgumentException;

    /// Resets the macro parameters to their defaults.
    void resetParams();

    /// Can this macro be applied on the given goals?
    /// This method should not make any changes but check if the macro can be applied or not on the
    /// given goals.
    /// This method may be called from within the GUI thread and be compatible with that fact.
    ///
    /// @param proof the current [Proof] (not <code>null</code>)
    /// @param goals the goals (not <code>null</code>)
    /// @param posInOcc the position in occurrence (may be <code>null</code>)
    ///
    /// @return <code>true</code>, if the macro is allowed to be applied
    boolean canApplyTo(Proof proof, ImmutableList<Goal> goals,
            PosInOccurrence posInOcc);

    /// Can this macro be applied on the given node?
    /// This method should not make any changes but check if the macro can be applied or not on the
    /// given node.
    /// This method may be called from within the GUI thread and be compatible with that fact.
    /// This method must be implemented to have the same effect as calling
    /// [#canApplyTo(Proof, ImmutableList, PosInOccurrence)] with
    /// <code>node.proof()</code> as
    /// proof and all open goals below <code>node</code>.
    ///
    /// @param node the node (not <code>null</code>)
    /// @param posInOcc the position in occurrence (may be <code>null</code>)
    ///
    /// @return <code>true</code>, if the macro is allowed to be applied
    boolean canApplyTo(Node node, PosInOccurrence posInOcc);

    /// Apply this macro on the given goals.
    /// This method can change the proof by applying rules to it.
    /// This method is usually called from a dedicated thread and not the GUI thread. The thread it
    /// runs on may be interrupted. In this case, the macro may report the interruption by an
    /// [InterruptedException].
    /// A [ProverTaskListener] can be provided to which the progress will be reported. If no
    /// reports are desired, <code>null</code> cna be used for this parameter. If more than one
    /// listener is needed, consider combining them using a single listener object using the
    /// composite pattern.
    ///
    /// @param uic the [UserInterfaceControl] to use
    /// @param proof the current [Proof] (not <code>null</code>)
    /// @param goals the goals (not <code>null</code>)
    /// @param posInOcc the position in occurrence (may be <code>null</code>)
    /// @param listener the listener to use for progress reports (may be <code>null</code>)
    /// @throws InterruptedException if the application of the macro has been interrupted.
    ProofMacroFinishedInfo applyTo(UserInterfaceControl uic, Proof proof,
            ImmutableList<Goal> goals, PosInOccurrence posInOcc,
            ProverTaskListener listener)
            throws Exception;

    /// Apply this macro on the given node.
    /// This method can change the proof by applying rules to it.
    /// This method is usually called from a dedicated thread and not the GUI thread. The thread it
    /// runs on may be interrupted. In this case, the macro may report the interruption by an
    /// [InterruptedException].
    /// A [ProverTaskListener] can be provided to which the progress will be reported. If no
    /// reports are desired, <code>null</code> can be used for this parameter. If more than one
    /// listener is needed, consider combining them using a single listener object using the
    /// composite pattern.
    ///
    /// @param uic the [UserInterfaceControl] to use
    /// @param node the node (not <code>null</code>)
    /// @param posInOcc the position in occurrence (may be <code>null</code>)
    /// @param listener the listener to use for progress reports (may be <code>null</code>)
    /// @throws InterruptedException if the application of the macro has been interrupted.
    ProofMacroFinishedInfo applyTo(UserInterfaceControl uic, Node node,
            PosInOccurrence posInOcc, ProverTaskListener listener)
            throws Exception;

    /// This observer acts as intermediate instance between the reports by the strategy and the UI
    /// reporting progress.
    /// The number of total steps is computed and all local reports are translated in termini of the
    /// total number of steps such that continuous progress is reported.
    /// fixes #1356
    class ProgressBarListener extends ProofMacroListener {
        private final int numberGoals;
        private final int numberSteps;
        private int completedGoals;

        ProgressBarListener(String name, int numberGoals, int numberSteps, ProverTaskListener l) {
            super(name, l);
            this.numberGoals = numberGoals;
            this.numberSteps = numberSteps;
        }

        public ProgressBarListener(int size, int numberSteps, ProverTaskListener listener) {
            this("", size, numberSteps, listener);
        }

        @Override
        public void taskStarted(TaskStartedInfo info) {
            // assert size == numberSteps;
            String suffix = getMessageSuffix();
            super.taskStarted(
                new DefaultTaskStartedInfo(TaskStartedInfo.TaskKind.Macro, info.message() + suffix,
                    numberGoals * numberSteps));
            super.taskProgress(completedGoals * numberSteps);
        }

        protected String getMessageSuffix() {
            return " [" + (completedGoals + 1) + "/" + numberGoals + "]";
        }

        @Override
        public void taskProgress(int position) {
            super.taskProgress(completedGoals * numberSteps + position);
        }

        @Override
        public void taskFinished(TaskFinishedInfo info) {
            super.taskFinished(info);
            completedGoals++;
        }
    }
}
