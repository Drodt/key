/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.expr;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.abstraction.Never;
import org.key_project.rusty.ast.abstraction.Type;
import org.key_project.rusty.ast.visitor.Visitor;

import org.jspecify.annotations.NonNull;

public record EmptyPanic() implements Expr {
    @Override
    public Type type(Services services) {
        return Never.INSTANCE;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnEmptyPanic(this);
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("No child " + n);
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public @NonNull String toString() {
        return "panic!()";
    }
}
