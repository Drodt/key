/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.Function;
import org.key_project.rusty.ast.expr.Expr;
import org.key_project.rusty.ast.ty.RustType;
import org.key_project.rusty.ast.visitor.Visitor;

import org.jspecify.annotations.NonNull;

public record ConstDef(String name, RustType rustType, Expr expr, Function fn) implements Item {
    @Override
    public void visit(Visitor v) {
        v.performActionOnConstDef(this);
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0)
            return rustType;
        if (n == 1)
            return expr;
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int getChildCount() {
        return 2;
    }
}
