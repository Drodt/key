/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.java.expression;

import de.uka.ilkd.key.java.Expression;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.expression.literal.BooleanLiteral;
import de.uka.ilkd.key.java.reference.ExecutionContext;

import org.key_project.util.ExtList;


/**
 * An assignment is an operator with side-effects.
 */

public abstract class Assignment extends Operator implements ExpressionStatement {

    protected Assignment() {

    }

    /**
     * Constructor for the transformation of COMPOST ASTs to KeY.
     *
     * @param children the children of this AST element as KeY classes. In this case the order of
     *        the children is IMPORTANT. May contain: 2 of Expression (the first Expression as left
     *        hand side, the second as right hand side), Comments
     */
    protected Assignment(ExtList children) {
        super(children);
    }


    /**
     * Unary Assignment (e.g. +=, ++).
     *
     * @param lhs an expression.
     */
    protected Assignment(Expression lhs) {
        super(lhs);
    }

    /**
     * Assignment.
     *
     * @param lhs an expression.
     * @param rhs an expression.
     */
    protected Assignment(Expression lhs, Expression rhs) {
        super(lhs, rhs);
    }


    /**
     * Checks if this operator is left or right associative. Assignments are right associative.
     *
     * @return <CODE>true</CODE>, if the operator is left associative, <CODE>false</CODE> otherwise.
     */

    public boolean isLeftAssociative() {
        return false;
    }


    /**
     * retrieves the type of the assignment expression
     *
     * @param javaServ the Services offering access to the Java model
     * @param ec the ExecutionContext in which the expression is evaluated
     * @return the type of the assignment expression
     */
    public KeYJavaType getKeYJavaType(Services javaServ, ExecutionContext ec) {
        return getExpressionAt(0).getKeYJavaType(javaServ, ec);
    }


    /**
     * overriden from Operator
     */
    public String reuseSignature(Services services, ExecutionContext ec) {
        String base = super.reuseSignature(services, ec);
        Expression rhs;
        try {
            rhs = children.get(1);
        } catch (ArrayIndexOutOfBoundsException e) {
            // no second argument, e.g. PostIncrement
            return base;
        }
        if (rhs instanceof BooleanLiteral) {
            return base + "[" + rhs + "]";
        } else {
            return base;
        }
    }

}
