/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.expr;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.abstraction.Type;
import org.key_project.rusty.ast.visitor.Visitor;

import org.jspecify.annotations.NonNull;

public record ConstBlockExpression(BlockExpression block) implements Expr {
    @Override
    public @NonNull String toString() {
        return "const " + block;
    }

    @Override
    public Type type(Services services) {
        return block.type(services);
    }

    @Override
    public void visit(Visitor v) {

    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0)
            return block;
        throw new IndexOutOfBoundsException("ConstBlockExpression n=" + n);
    }

    @Override
    public int getChildCount() {
        return 1;
    }
}
