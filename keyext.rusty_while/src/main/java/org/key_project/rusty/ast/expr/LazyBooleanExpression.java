/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.expr;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.ast.visitor.Visitor;

import org.jspecify.annotations.NonNull;

public record LazyBooleanExpression(Expr left, Operator op, Expr right) implements Expr {
    @Override
    public void visit(Visitor v) {
        v.performActionOnLazyBooleanExpression(this);
    }

    @Override
    public  @NonNull SyntaxElement getChild(int n) {
        return switch (n) {
            case 0 -> left;
            case 1 -> right;
            default -> throw new IndexOutOfBoundsException(
                    "ComparisonExpression has only 2 children");
        };
    }

    @Override
    public int getChildCount() {
        return 2;
    }

    @Override
    public String toString() {
        return left + " " + op + " " + right;
    }

    public enum Operator {
        And, Or;

        @Override
        public String toString() {
            return switch (this) {
                case And -> "&&";
                case Or -> "||";
            };
        }
    }}