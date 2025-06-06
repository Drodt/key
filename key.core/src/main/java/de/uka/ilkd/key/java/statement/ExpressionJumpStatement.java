/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.java.statement;

import de.uka.ilkd.key.java.Expression;
import de.uka.ilkd.key.java.ExpressionContainer;
import de.uka.ilkd.key.java.ProgramElement;

import org.key_project.util.ExtList;

/**
 * Expression jump statement.
 *
 * @author <TT>AutoDoc</TT>
 */

public abstract class ExpressionJumpStatement extends JumpStatement implements ExpressionContainer {

    /**
     * Expression.
     */

    protected final Expression expression;

    /**
     * Expression jump statement. May contain: an Expression (as expression of the
     * ExpressionJumpStatement), Comments
     */
    protected ExpressionJumpStatement(ExtList children) {
        super(children);
        expression = children.get(Expression.class);
    }

    /**
     * Expression jump statement.
     */
    protected ExpressionJumpStatement() {
        expression = null;
    }

    /**
     * Expression jump statement.
     *
     * @param expr an Expression used to jump
     */
    protected ExpressionJumpStatement(Expression expr) {
        expression = expr;
    }

    /**
     * Get the number of expressions in this container.
     *
     * @return the number of expressions.
     */
    public int getExpressionCount() {
        return (expression != null) ? 1 : 0;
    }

    /**
     * Return the expression at the specified index in this node's "virtual" expression array.
     *
     * @param index an index for an expression.
     * @return the expression with the given index.
     * @exception ArrayIndexOutOfBoundsException if <tt>index</tt> is out of bounds.
     */
    public Expression getExpressionAt(int index) {
        if (expression != null && index == 0) {
            return expression;
        }
        throw new ArrayIndexOutOfBoundsException();
    }

    /**
     * Get expression.
     *
     * @return the expression.
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * Returns the number of children of this node.
     *
     * @return an int giving the number of children of this node
     */
    public int getChildCount() {
        return (expression != null) ? 1 : 0;
    }

    /**
     * Returns the child at the specified index in this node's "virtual" child array
     *
     * @param index an index into this node's "virtual" child array
     * @return the program element at the given position
     * @exception ArrayIndexOutOfBoundsException if <tt>index</tt> is out of bounds
     */
    public ProgramElement getChildAt(int index) {
        if (expression != null) {
            if (index == 0) {
                return expression;
            }
        }
        throw new ArrayIndexOutOfBoundsException();
    }
}
