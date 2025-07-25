/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.expr;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.abstraction.Type;
import org.key_project.rusty.ast.visitor.Visitor;

import org.jspecify.annotations.NonNull;

public final class RepeatedArrayExpression implements Expr {
    private final Expr expr;
    private final Expr size;
    private final Type ty;

    public RepeatedArrayExpression(Expr expr, Expr size, Type ty) {
        this.expr = expr;
        this.size = size;
        this.ty = ty;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnRepeatedArrayExpression(this);
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0)
            return expr;
        if (n == 1)
            return size;
        throw new IndexOutOfBoundsException("RepeatedArrayExpression has only 2 children");
    }

    @Override
    public int getChildCount() {
        return 2;
    }

    @Override
    public @NonNull String toString() {
        return "[" + expr + "; " + size + "]";
    }

    @Override
    public Type type(Services services) {
        return ty;
    }

    public Expr expr() {
        return expr;
    }

    public Expr size() {
        return size;
    }

    public Type ty() {
        return ty;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var that = (RepeatedArrayExpression) obj;
        return Objects.equals(this.expr, that.expr) &&
                Objects.equals(this.size, that.size) &&
                Objects.equals(this.ty, that.ty);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expr, size, ty);
    }

}
