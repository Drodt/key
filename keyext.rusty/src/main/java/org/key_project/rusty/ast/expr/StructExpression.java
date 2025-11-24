/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.expr;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.QPath;
import org.key_project.rusty.ast.abstraction.Type;
import org.key_project.rusty.ast.visitor.Visitor;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record StructExpression(QPath path, ImmutableArray<StructExprField> fields,
        @Nullable StructTail tail) implements Expr {
    @Override
    public void visit(Visitor v) {
        v.performActionOnStructExpression(this);
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0) {
            return path;
        }
        n--;
        if (n < fields.size())
            return Objects.requireNonNull(fields.get(n));
        n -= fields.size();
        if (n == 0 && tail != null) {
            return tail;
        }
        throw new IndexOutOfBoundsException(
            String.format("%s has only %d children", getClass(), getChildCount()));
    }

    @Override
    public int getChildCount() {
        return 1 + fields.size();
    }

    @Override
    public @NonNull String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(path).append(" {\n");
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sb.append(",\n");
            }
            sb.append('\t').append(fields.get(i));
            if (i == fields.size() - 1 && tail != null) {
                sb.append(tail);
            }
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public Type type(Services services) {
        throw new UnsupportedOperationException();
    }
}
