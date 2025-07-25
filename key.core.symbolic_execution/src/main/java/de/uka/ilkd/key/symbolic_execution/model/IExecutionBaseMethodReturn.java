/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.symbolic_execution.model;

import de.uka.ilkd.key.java.SourceElement;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.proof.init.ProofInputException;

/**
 * Defines the common interface of {@link IExecutionMethodReturn} and
 * {@link IExecutionExceptionalMethodReturn}.
 *
 * @author Martin Hentschel
 */
public interface IExecutionBaseMethodReturn<S extends SourceElement> extends IExecutionNode<S> {
    /**
     * A reference to the {@link IExecutionMethodCall} which is now returned.
     *
     * @return The call of the now returned method.
     */
    IExecutionMethodCall getMethodCall();

    /**
     * Returns a human readable signature which describes this element.
     *
     * @return The human readable signature which describes this element.
     * @throws ProofInputException Occurred Exception.
     */
    String getSignature() throws ProofInputException;

    /**
     * Returns the condition under which this method return is reached from the calling
     * {@link IExecutionMethodCall}.
     *
     * @return The method return condition to reach this node from its {@link IExecutionMethodCall}
     *         as {@link JTerm}.
     */
    JTerm getMethodReturnCondition() throws ProofInputException;

    /**
     * Returns the human readable condition under which this method return is reached from the
     * calling {@link IExecutionMethodCall}.
     *
     * @return The human readable method return condition to reach this node from its
     *         {@link IExecutionMethodCall}.
     */
    String getFormattedMethodReturnCondition() throws ProofInputException;

    /**
     * Returns the variable value pairs of the state when the method has been called.
     *
     * @return The variable value pairs.
     */
    IExecutionVariable[] getCallStateVariables() throws ProofInputException;
}
