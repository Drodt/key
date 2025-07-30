/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.expr;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.abstraction.Type;
import org.key_project.rusty.ast.visitor.Visitor;

public record EmptyPanic() implements Expr {
    @Override
    public Type type(Services services) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(Visitor v) {

    }

    @Override
    public SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("No child " + n);
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public String toString() {
        return "panic!()";
    }
}
