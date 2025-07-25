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

public final class IndexExpression implements Expr {
    private final Expr base;
    private final Expr index;
    private final Type ty;

    public IndexExpression(Expr base, Expr index, Type ty) {
        this.base = base;
        this.index = index;
        this.ty = ty;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnIndexExpression(this);
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0)
            return base;
        if (n == 1)
            return index;
        throw new IndexOutOfBoundsException("IndexExpression has only 2 children: " + n);
    }

    @Override
    public int getChildCount() {
        return 2;
    }

    @Override
    public String toString() {
        return base + "[" + index + "]";
    }

    @Override
    public Type type(Services services) {
        return ty;
    }

    public Expr base() {
        return base;
    }

    public Expr index() {
        return index;
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
        var that = (IndexExpression) obj;
        return Objects.equals(this.base, that.base) &&
                Objects.equals(this.index, that.index) &&
                Objects.equals(this.ty, that.ty);
    }

    @Override
    public int hashCode() {
        return Objects.hash(base, index, ty);
    }

}
