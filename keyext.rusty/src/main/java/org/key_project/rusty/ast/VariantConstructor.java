/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.ast.visitor.Visitor;

public record VariantConstructor() implements Def {
    @Override
    public void visit(Visitor v) {
        v.performActionOnVariantConstructor(this);
    }

    @Override
    public SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("Invalid index: " + n);
    }

    @Override
    public int getChildCount() {
        return 0;
    }
}
