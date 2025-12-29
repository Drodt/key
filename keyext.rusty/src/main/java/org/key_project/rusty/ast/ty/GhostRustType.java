/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.ty;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.ast.abstraction.GhostType;
import org.key_project.rusty.ast.abstraction.Type;
import org.key_project.rusty.ast.visitor.Visitor;

import org.jspecify.annotations.NonNull;

public record GhostRustType(RustType inner) implements RustType {
    @Override
    public Type type() {
        return GhostType.get(inner.type());
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnGhostRustType(this);
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0)
            return inner;
        throw new IndexOutOfBoundsException("GhostRustType has only one child");
    }

    @Override
    public int getChildCount() {
        return 1;
    }
}
