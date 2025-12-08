/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.expr;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.abstraction.ArrayType;
import org.key_project.rusty.ast.abstraction.IntArrayLen;
import org.key_project.rusty.ast.abstraction.Type;
import org.key_project.rusty.ast.visitor.Visitor;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;

public final class ArrayExpression implements Expr {
    private final ImmutableArray<Expr> elements;
    private final Type type;

    public ArrayExpression(ImmutableArray<Expr> elements, Type type) {
        this.elements = elements;
        this.type = type;
    }

    public ArrayExpression(ExtList children, Services services) {
        elements = new ImmutableArray<>(children.collect(Expr.class));
        type = ArrayType.getInstance(elements.get(0).type(services),
            new IntArrayLen(elements().size()), services);
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnEnumeratedArrayExpression(this);
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        return Objects.requireNonNull(elements.get(n));
    }

    @Override
    public int getChildCount() {
        return elements.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(elements.get(i));
        }
        sb.append("]");
        return sb.toString();
    }


    @Override
    public Type type(Services services) {
        return type;
    }

    public ImmutableArray<Expr> elements() {
        return elements;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var that = (ArrayExpression) obj;
        return Objects.equals(this.elements, that.elements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elements);
    }

}
