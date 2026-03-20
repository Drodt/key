/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.macro;

import org.key_project.prover.engine.ProverTaskListener;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.rusty.control.UserInterfaceControl;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Node;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.settings.ProofSettings;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

public abstract class AbstractProofMacro implements ProofMacro {
    /// {@inheritDoc}
    /// By default, proof macros do not support scripts, thus <code>null</code> is returned.
    @Override
    public String getScriptCommandName() {
        return null;
    }

    @Override
    public boolean hasParameter(String paramName) {
        return false;
    }

    @Override
    public void setParameter(String paramName, String paramValue) throws IllegalArgumentException {
        throw new IllegalArgumentException(
            String.format("There is no parameter of name %s in macro %s", paramName,
                this.getClass().getSimpleName()));
    }

    @Override
    public void resetParams() {
    }

    @Override
    public boolean canApplyTo(Node node, PosInOccurrence posInOcc) {
        return canApplyTo(node.proof(), getGoals(node), posInOcc);
    }

    @Override
    public ProofMacroFinishedInfo applyTo(UserInterfaceControl uic, Node node,
            PosInOccurrence posInOcc, ProverTaskListener listener)
            throws Exception {
        return applyTo(uic, node.proof(), getGoals(node), posInOcc, listener);
    }

    /// Gets the maximum number of rule applications allowed for a macro. The implementation is the
    /// maximum amount of proof steps for automatic mode.
    ///
    /// @return the maximum number of rule applications allowed for this macro
    final protected int getMaxSteps(Proof proof) {
        final int steps;
        if (proof != null) {
            steps = proof.getSettings().getStrategySettings().getMaxSteps();
        } else {
            steps = ProofSettings.DEFAULT_SETTINGS.getStrategySettings().getMaxSteps();
        }
        return steps;
    }

    private static ImmutableList<Goal> getGoals(Node node) {
        if (node == null) {
            // can happen during initialization
            return ImmutableSLList.nil();
        } else {
            return node.proof().getSubtreeEnabledGoals(node);
        }
    }
}
