/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.symbolic_execution.model;

import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.proof.init.ProofInputException;
import de.uka.ilkd.key.symbolic_execution.model.impl.ExecutionValue;

/**
 * <p>
 * A value of an {@link IExecutionVariable}, e.g. the method parameter {@code int x = 42;} will have
 * the variable value pair {@code x = 42}. This class represents the value ({@code 42}).
 * </p>
 * <p>
 * The default implementation is {@link ExecutionValue} which is instantiated lazily by the
 * {@link IExecutionVariable} implementation.
 * </p>
 *
 * @author Martin Hentschel
 * @see IExecutionVariable
 * @see ExecutionValue
 */
public interface IExecutionValue extends IExecutionElement {
    /**
     * Returns the condition under which the variable ({@link #getVariable()}) has this value.
     *
     * @return The condition.
     * @throws ProofInputException Occurred Exception.
     */
    JTerm getCondition() throws ProofInputException;

    /**
     * Returns the condition under which the variable ({@link #getVariable()}) has this value as
     * human readable {@link String}.
     *
     * @return The condition as human readable {@link String}.
     * @throws ProofInputException Occurred Exception.
     */
    String getConditionString() throws ProofInputException;

    /**
     * <p>
     * Checks if the value is unknown.
     * </p>
     * <p>
     * Imagine the following class:
     *
     * <pre>
     * <code>
     * public class A {
     *    private A next;
     *
     *    public int main() {
     *       return 42; // breakpoint
     *    }
     * }
     * </code>
     * </pre>
     *
     * If the main method is debugged symbolically and stopped at the statement marked with the
     * comment {@code // breakpoint} the field {@code self.next} has the symbolic value
     * {@code SVself.next}. And its field {@code self.next.next} has again a symbolic value
     * {@code SVself.next.next}. This chain is infinite deep. But on the other side the Sequent
     * contains no information about {@code self.next} so the symbolic value can be {@code null} or
     * a concrete object. Such symbolic values which are not founded in the Sequent are treated in
     * the symbolic execution API as "unknown" to avoid infinite deep value hierarchies if they are
     * not cyclic.
     * </p>
     *
     * @return {@code true} value is unknown, {@code false} value is known.
     */
    boolean isValueUnknown() throws ProofInputException;

    /**
     * Returns the value of the variable.
     *
     * @return The value of the variable.
     */
    JTerm getValue() throws ProofInputException;

    /**
     * Returns the value of the variable as human readable string representation.
     *
     * @return The value of the variable as human readable string representation.
     */
    String getValueString() throws ProofInputException;

    /**
     * <p>
     * Checks if the value represents an object or an attribute of the object defined by the parent
     * {@link IExecutionValue}.
     * </p>
     * <p>
     * All values which are not a basic datatype (integer, boolean, ...) and not an instance of a
     * library class will be treated as object.
     * </p>
     *
     * @return {@code true} is an object, {@code false} is an attribute of the object defined by the
     *         parent {@link IExecutionValue}.
     * @throws ProofInputException Occurred Exception.
     */
    boolean isValueAnObject() throws ProofInputException;

    /**
     * Returns the type of the variable as human readable string.
     *
     * @return The type of the variable as human readable string.
     */
    String getTypeString() throws ProofInputException;

    /**
     * Returns the parent {@link IExecutionVariable}.
     *
     * @return The parent {@link IExecutionVariable}.
     */
    IExecutionVariable getVariable();

    /**
     * Returns contained child variables which forms complex data types.
     *
     * @return The contained child variables.
     */
    IExecutionVariable[] getChildVariables() throws ProofInputException;

    /**
     * Returns all available {@link IExecutionConstraint}s.
     *
     * @return The available {@link IExecutionConstraint}s.
     * @throws ProofInputException Occurred Exception.
     */
    IExecutionConstraint[] getConstraints() throws ProofInputException;
}
