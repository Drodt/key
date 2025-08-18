/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.ty;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.ast.abstraction.PtrType;
import org.key_project.rusty.ast.abstraction.Type;
import org.key_project.rusty.ast.visitor.Visitor;

import org.jspecify.annotations.NonNull;

public class PtrRustType implements RustType {
    private final Type type;
    private final RustType inner;

    public PtrRustType(RustType inner) {
        this.inner = inner;
        type = PtrType.get(inner.type());
    }

    public RustType getInner() {
        return inner;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnPtrRustType(this);
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0)
            return inner;
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int getChildCount() {
        return 1;
    }
}
