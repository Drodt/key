/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.ast;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.ast.ty.RustType;
import org.key_project.rusty.ast.visitor.Visitor;

import org.jspecify.annotations.Nullable;

public record QPathResolved(@Nullable RustType selfTy, Path<Res> path) implements QPath {
    @Override
    public void visit(Visitor v) {

    }

    @Override
    public SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("No child " + n + " of " + this);
    }

    @Override
    public int getChildCount() {
        return 0;
    }
}
