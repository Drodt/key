/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.ty;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.ast.abstraction.SliceType;
import org.key_project.rusty.ast.abstraction.Type;
import org.key_project.rusty.ast.visitor.Visitor;

import org.jspecify.annotations.NonNull;

public class SliceRustType implements RustType {
    private final RustType inner;
    private final Type type;

    public SliceRustType(RustType rustType) {
        inner = rustType;
        type = SliceType.get(inner.type());
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
        v.performActionOnSliceRustType(this);
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
